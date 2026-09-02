package com.app.admin.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.app.admin.model.MedicalSubject;
import com.app.admin.model.Technology;
import com.app.admin.repository.MedicalSubjectRepository;
import com.app.admin.repository.TechnologyRepository;
import com.app.admin.repository.FieldRepository;
import com.app.admin.exception.ResourceNotFoundException;

@Service
public class TopicImageService {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final MediaStorage storage;
    private final TechnologyRepository technologies;
    private final MedicalSubjectRepository medicalSubjects;
    private final FieldRepository fields;
    private final TopicImageLifecycle imageLifecycle;
    private final long maxBytes;
    private final long maxPixelCount;
    private final Map<String, Integer> variants;

    public TopicImageService(MediaStorage storage, TechnologyRepository technologies,
            MedicalSubjectRepository medicalSubjects, FieldRepository fields, TopicImageLifecycle imageLifecycle,
            @Value("${media.topic-image-max-upload-bytes:2097152}") long maxBytes,
            @Value("${media.max-pixel-count:40000000}") long maxPixelCount,
            @Value("${media.image.thumb-width:160}") int thumbWidth,
            @Value("${media.image.small-width:480}") int smallWidth,
            @Value("${media.image.medium-width:960}") int mediumWidth) {
        this.storage = storage;
        this.technologies = technologies;
        this.medicalSubjects = medicalSubjects;
        this.fields = fields;
        this.imageLifecycle = imageLifecycle;
        this.maxBytes = maxBytes;
        this.maxPixelCount = maxPixelCount;
        this.variants = orderedVariants(thumbWidth, smallWidth, mediumWidth);
    }

    @Transactional
    public Technology updateTechnologyImage(Long id, MultipartFile file) {
        Technology topic = technologies.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technology", id));
        String oldKey = topic.getImageKey();
        Stored stored = store("technology", topic.getId(), file);
        cleanUpNewOnRollback(stored.keys());
        topic.setImageKey(stored.preferredKey());
        topic.setImageUrl(storage.publicUrl(stored.preferredKey()));
        Technology saved = technologies.saveAndFlush(topic);
        deleteOldAfterCommit(oldKey, stored.preferredKey());
        return saved;
    }

    @Transactional
    public MedicalSubject updateMedicalSubjectImage(Long id, MultipartFile file) {
        MedicalSubject topic = medicalSubjects.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSubject", id));
        String oldKey = topic.getImageKey();
        // Keep this validation because MEDICAL is shared domain data, even though
        // the blob key uses a stable topic type rather than a mutable field id.
        fields.findByCodeIgnoreCase("MEDICAL")
                .orElseThrow(() -> new IllegalStateException("Field with code MEDICAL is required"));
        Stored stored = store("medical-subject", topic.getId(), file);
        cleanUpNewOnRollback(stored.keys());
        topic.setImageKey(stored.preferredKey());
        topic.setImageUrl(storage.publicUrl(stored.preferredKey()));
        MedicalSubject saved = medicalSubjects.saveAndFlush(topic);
        deleteOldAfterCommit(oldKey, stored.preferredKey());
        return saved;
    }

    public String imageUrl(String imageKey) {
        return storage.publicUrl(imageKey);
    }

    private Stored store(String topicType, Long topicId, MultipartFile file) {
        validate(file);
        BufferedImage source = decode(file);
        String prefix = "master/topics/" + topicType + "/" + topicId + "/" + UUID.randomUUID() + "/";
        List<String> uploaded = new ArrayList<>();
        try {
            for (Map.Entry<String, Integer> variant : variants.entrySet()) {
                byte[] content = encodeWebp(resize(source, variant.getValue()));
                String key = prefix + variant.getKey() + ".webp";
                storage.store(key, new ByteArrayInputStream(content), content.length, "image/webp");
                uploaded.add(key);
            }
            return new Stored(prefix + "small.webp", List.copyOf(uploaded));
        } catch (RuntimeException ex) {
            deleteAll(uploaded);
            throw ex;
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Image file is required");
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("Image exceeds the upload limit");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
    }

    private BufferedImage decode(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Uploaded file is not a valid image");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if ((long) width * height > maxPixelCount) {
                        throw new IllegalArgumentException("Image dimensions exceed the pixel limit");
                    }
                    BufferedImage image = reader.read(0);
                    if (image == null) throw new IllegalArgumentException("Uploaded file is not a valid image");
                    return image;
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not decode uploaded image", ex);
        }
    }

    private BufferedImage resize(BufferedImage source, int maxWidth) {
        int width = Math.min(source.getWidth(), maxWidth);
        int height = Math.max(1, (int) Math.round(source.getHeight() * (width / (double) source.getWidth())));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] encodeWebp(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "webp", output)) {
                throw new IllegalStateException("WebP image encoder is unavailable");
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not encode image as WebP", ex);
        }
    }

    private Map<String, Integer> orderedVariants(int thumbWidth, int smallWidth, int mediumWidth) {
        if (thumbWidth <= 0 || smallWidth <= 0 || mediumWidth <= 0) {
            throw new IllegalArgumentException("Image variant widths must be positive");
        }
        Map<String, Integer> configured = new LinkedHashMap<>();
        configured.put("thumb", thumbWidth);
        configured.put("small", smallWidth);
        configured.put("medium", mediumWidth);
        return Map.copyOf(configured);
    }

    private void deleteOldAfterCommit(String oldKey, String newKey) {
        if (oldKey == null || oldKey.equals(newKey)) return;
        imageLifecycle.deleteAfterCommit(oldKey);
    }

    private void cleanUpNewOnRollback(List<String> newKeys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteAll(newKeys);
            }
        });
    }

    private void deleteAll(List<String> keys) {
        for (String key : keys) storage.delete(key);
    }

    private record Stored(String preferredKey, List<String> keys) {}
}
