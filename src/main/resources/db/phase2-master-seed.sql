-- Idempotent Phase 2 starter data. Codes match the values used by TechAppBE.
-- Install timestamp defaults even when Hibernate created these tables first.
DO $$
DECLARE table_name TEXT;
BEGIN
  FOREACH table_name IN ARRAY ARRAY['event_delivery_mode','training_delivery_mode','event_type','training_type','opportunity_type','organization_type'] LOOP
    EXECUTE format('ALTER TABLE public.%I ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP', table_name);
    EXECUTE format('ALTER TABLE public.%I ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP', table_name);
  END LOOP;
END $$;

INSERT INTO public.event_delivery_mode (code,name,description,display_order,active) VALUES
('ONLINE','Online','Event conducted completely online',1,TRUE),
('IN_PERSON','In Person','Event conducted at a physical venue',2,TRUE),
('HYBRID','Hybrid','Event available both online and at a physical venue',3,TRUE)
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,
display_order=EXCLUDED.display_order,active=EXCLUDED.active,updated_at=CURRENT_TIMESTAMP;

INSERT INTO public.training_delivery_mode (code,name,description,display_order,active) VALUES
('ONLINE_LIVE','Online Live','Instructor-led training delivered live online',1,TRUE),
('CLASSROOM','Classroom','Instructor-led training at a classroom or physical venue',2,TRUE),
('HYBRID','Hybrid','Training combining online and classroom delivery',3,TRUE),
('SELF_PACED','Self-paced','Learner completes training independently at their own pace',4,TRUE),
('RECORDED','Recorded','Training delivered through pre-recorded sessions',5,TRUE),
('ON_SITE','On-site','Training delivered at the customer or organization location',6,TRUE),
('BLENDED','Blended','Training combining multiple synchronous and asynchronous methods',7,TRUE),
('BOOTCAMP','Bootcamp','Intensive training delivered over a concentrated period',8,TRUE)
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,
display_order=EXCLUDED.display_order,active=EXCLUDED.active,updated_at=CURRENT_TIMESTAMP;

INSERT INTO public.event_type (code,name,display_order,active) VALUES
('WEBINAR','Webinar',1,TRUE),('HACKATHON','Hackathon',2,TRUE),('CODING_CHALLENGE','Coding Challenge',3,TRUE),
('CONFERENCE','Conference',4,TRUE),('WORKSHOP','Workshop',5,TRUE),('MEETUP','Meetup',6,TRUE),
('PRESS_RELEASE','Press Release',7,TRUE),('PRODUCT_LAUNCH','Product Launch',8,TRUE),('SEMINAR','Seminar',9,TRUE),('OTHER','Other',10,TRUE)
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,display_order=EXCLUDED.display_order,active=EXCLUDED.active,updated_at=CURRENT_TIMESTAMP;

INSERT INTO public.training_type (code,name,display_order,active) VALUES
('ONE_TIME','One Time',1,TRUE),('COURSE','Course',2,TRUE),('FLEXIBLE','Flexible',3,TRUE)
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,display_order=EXCLUDED.display_order,active=EXCLUDED.active,updated_at=CURRENT_TIMESTAMP;

INSERT INTO public.opportunity_type (code,name,display_order,active) VALUES
('JOB','Job',1,TRUE),('INTERNSHIP','Internship',2,TRUE),('FREELANCE','Freelance',3,TRUE),('CONTRACT','Contract',4,TRUE),('VOLUNTEER','Volunteer',5,TRUE),('OTHER','Other',6,TRUE)
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,display_order=EXCLUDED.display_order,active=EXCLUDED.active,updated_at=CURRENT_TIMESTAMP;

INSERT INTO public.organization_type (code,name,display_order,active) VALUES
('INDIVIDUAL','Individual',1,TRUE),('COMPANY','Company',2,TRUE),('INSTITUTE','Institute',3,TRUE),('UNIVERSITY','University',4,TRUE),('NON_PROFIT','Non-profit',5,TRUE),('GOVERNMENT','Government',6,TRUE),('OTHER','Other',7,TRUE)
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,display_order=EXCLUDED.display_order,active=EXCLUDED.active,updated_at=CURRENT_TIMESTAMP;
