-- Idempotent starter data for the application's initial India deployment.
-- It contains all Indian states/union territories and scheduled languages,
-- plus representative districts/cities. Add the production-approved locality
-- dataset using the same natural keys; reruns will update rather than duplicate.

INSERT INTO public.country
    (code, name, phone_code, display_order, active, created_at, updated_at) VALUES
('IND', 'India', '+91', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USA', 'United States', '+1', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GBR', 'United Kingdom', '+44', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ARE', 'United Arab Emirates', '+971', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CAN', 'Canada', '+1', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('AUS', 'Australia', '+61', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, phone_code = EXCLUDED.phone_code,
    display_order = EXCLUDED.display_order, active = EXCLUDED.active, updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.state
    (country_id, code, name, display_order, active, created_at, updated_at)
SELECT c.id, v.code, v.name, v.display_order, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM public.country c
CROSS JOIN (VALUES
('AP','Andhra Pradesh',1),('AR','Arunachal Pradesh',2),('AS','Assam',3),('BR','Bihar',4),
('CG','Chhattisgarh',5),('GA','Goa',6),('GJ','Gujarat',7),('HR','Haryana',8),
('HP','Himachal Pradesh',9),('JH','Jharkhand',10),('KA','Karnataka',11),('KL','Kerala',12),
('MP','Madhya Pradesh',13),('MH','Maharashtra',14),('MN','Manipur',15),('ML','Meghalaya',16),
('MZ','Mizoram',17),('NL','Nagaland',18),('OD','Odisha',19),('PB','Punjab',20),
('RJ','Rajasthan',21),('SK','Sikkim',22),('TN','Tamil Nadu',23),('TS','Telangana',24),
('TR','Tripura',25),('UP','Uttar Pradesh',26),('UK','Uttarakhand',27),('WB','West Bengal',28),
('AN','Andaman and Nicobar Islands',29),('CH','Chandigarh',30),('DH','Dadra and Nagar Haveli and Daman and Diu',31),
('DL','Delhi',32),('JK','Jammu and Kashmir',33),('LA','Ladakh',34),('LD','Lakshadweep',35),('PY','Puducherry',36)
) AS v(code, name, display_order)
WHERE c.code = 'IND'
ON CONFLICT (country_id, code) DO UPDATE SET name = EXCLUDED.name,
    display_order = EXCLUDED.display_order, active = EXCLUDED.active, updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.district
    (state_id, code, name, display_order, active, created_at, updated_at)
SELECT s.id, v.code, v.name, v.display_order, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM public.state s
JOIN public.country c ON c.id = s.country_id
JOIN (VALUES
('KL','TVM','Thiruvananthapuram',1),('KL','EKM','Ernakulam',2),('KL','KKD','Kozhikode',3),
('TN','CHE','Chennai',1),('TN','CBE','Coimbatore',2),('TN','MDU','Madurai',3),
('KA','BLR','Bengaluru Urban',1),('KA','MYS','Mysuru',2),
('MH','MUM','Mumbai City',1),('MH','PUN','Pune',2),
('TS','HYD','Hyderabad',1),('DL','CEN','Central Delhi',1)
) AS v(state_code, code, name, display_order) ON v.state_code = s.code
WHERE c.code = 'IND'
ON CONFLICT (state_id, name) DO UPDATE SET code = EXCLUDED.code,
    display_order = EXCLUDED.display_order, active = EXCLUDED.active, updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.city
    (district_id, code, name, postal_code, display_order, active, created_at, updated_at)
SELECT d.id, v.code, v.name, v.postal_code, v.display_order, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM public.district d
JOIN public.state s ON s.id = d.state_id
JOIN public.country c ON c.id = s.country_id
JOIN (VALUES
('KL','Thiruvananthapuram','TVM','Thiruvananthapuram','695001',1),
('KL','Ernakulam','KOC','Kochi','682001',1),('KL','Kozhikode','CLT','Kozhikode','673001',1),
('TN','Chennai','MAA','Chennai','600001',1),('TN','Coimbatore','CJB','Coimbatore','641001',1),
('TN','Madurai','IXM','Madurai','625001',1),('KA','Bengaluru Urban','BLR','Bengaluru','560001',1),
('KA','Mysuru','MYQ','Mysuru','570001',1),('MH','Mumbai City','BOM','Mumbai','400001',1),
('MH','Pune','PNQ','Pune','411001',1),('TS','Hyderabad','HYD','Hyderabad','500001',1),
('DL','Central Delhi','DEL','New Delhi','110001',1)
) AS v(state_code, district_name, code, name, postal_code, display_order)
    ON v.state_code = s.code AND v.district_name = d.name
WHERE c.code = 'IND'
ON CONFLICT (district_id, name) DO UPDATE SET code = EXCLUDED.code, postal_code = EXCLUDED.postal_code,
    display_order = EXCLUDED.display_order, active = EXCLUDED.active, updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.language
    (code, name, native_name, display_order, active, created_at, updated_at) VALUES
('as','Assamese','অসমীয়া',1,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('bn','Bengali','বাংলা',2,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('brx','Bodo','बड़ो',3,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('doi','Dogri','डोगरी',4,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('gu','Gujarati','ગુજરાતી',5,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('hi','Hindi','हिन्दी',6,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('kn','Kannada','ಕನ್ನಡ',7,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('ks','Kashmiri','کٲشُر',8,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('kok','Konkani','कोंकणी',9,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('mai','Maithili','मैथिली',10,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('ml','Malayalam','മലയാളം',11,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('mni','Manipuri','মৈতৈলোন্',12,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('mr','Marathi','मराठी',13,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('ne','Nepali','नेपाली',14,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('or','Odia','ଓଡ଼ିଆ',15,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pa','Punjabi','ਪੰਜਾਬੀ',16,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('sa','Sanskrit','संस्कृतम्',17,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('sat','Santali','ᱥᱟᱱᱛᱟᱲᱤ',18,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('sd','Sindhi','سنڌي',19,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('ta','Tamil','தமிழ்',20,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('te','Telugu','తెలుగు',21,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('ur','Urdu','اردو',22,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),('en','English','English',23,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, native_name = EXCLUDED.native_name,
    display_order = EXCLUDED.display_order, active = EXCLUDED.active, updated_at = CURRENT_TIMESTAMP;
