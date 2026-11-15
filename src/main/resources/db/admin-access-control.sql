/*
 * Create or upgrade the earlier Admin prototype schema to the production
 * Admin auth/RBAC model. Safe to reapply to an existing installation.
 * Does not alter public.users or add any FK from Admin identities to users.
 * The literal OWNER TO statements from the supplied workstation script are
 * intentionally omitted; ownership is deployment-specific.
 */
BEGIN;

CREATE TABLE IF NOT EXISTS public.admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100), email VARCHAR(255), password_hash VARCHAR(255),
    first_name VARCHAR(100), last_name VARCHAR(100), mobile_number VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE, last_login_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS username VARCHAR(100);
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(30);
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.admin_users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_users' AND column_name='active') THEN
        /* is_active is added with TRUE on first upgrade. Never overwrite a
           later explicit disable if this idempotent script is rerun. */
        UPDATE public.admin_users SET is_active=active WHERE active=FALSE AND is_active=TRUE;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_users' AND column_name='display_name') THEN
        UPDATE public.admin_users SET first_name=COALESCE(first_name,split_part(display_name,' ',1)),
            last_name=COALESCE(last_name,NULLIF(substr(display_name,length(split_part(display_name,' ',1))+2),''));
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_users' AND column_name='must_change_password') THEN
        UPDATE public.admin_users SET must_change_password=TRUE WHERE must_change_password IS NULL;
    END IF;
END $$;
UPDATE public.admin_users SET username=COALESCE(NULLIF(username,''), 'admin_'||id::text),
    email=COALESCE(NULLIF(email,''), username||'@invalid.local'),
    password_hash=COALESCE(password_hash,'!MUST_RESET!'),
    is_active=COALESCE(is_active,TRUE), created_at=COALESCE(created_at,CURRENT_TIMESTAMP),
    updated_at=COALESCE(updated_at,CURRENT_TIMESTAMP);
UPDATE public.admin_users SET username=lower(username),email=lower(email);
ALTER TABLE public.admin_users ALTER COLUMN username SET NOT NULL;
ALTER TABLE public.admin_users ALTER COLUMN email SET NOT NULL;
ALTER TABLE public.admin_users ALTER COLUMN password_hash SET NOT NULL;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_users' AND column_name='display_name') THEN
        ALTER TABLE public.admin_users ALTER COLUMN display_name DROP NOT NULL;
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_users_username ON public.admin_users(username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_users_email ON public.admin_users(email);
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_users_username_ci ON public.admin_users(lower(username));
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_users_email_ci ON public.admin_users(lower(email));
CREATE INDEX IF NOT EXISTS idx_admin_users_active ON public.admin_users(is_active);

CREATE TABLE IF NOT EXISTS public.admin_roles (
    id BIGSERIAL PRIMARY KEY, code VARCHAR(60), name VARCHAR(100) NOT NULL,
    description VARCHAR(500), is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE public.admin_roles ADD COLUMN IF NOT EXISTS code VARCHAR(60);
ALTER TABLE public.admin_roles ADD COLUMN IF NOT EXISTS name VARCHAR(100);
ALTER TABLE public.admin_roles ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE public.admin_roles ADD COLUMN IF NOT EXISTS is_system_role BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.admin_roles ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE public.admin_roles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_roles' AND column_name='system_role') THEN
        UPDATE public.admin_roles SET is_system_role=system_role WHERE system_role IS NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_roles' AND column_name='active') THEN
        UPDATE public.admin_roles SET is_active=active WHERE active=FALSE AND is_active=TRUE;
    END IF;
END $$;
UPDATE public.admin_roles SET code=COALESCE(code,upper(regexp_replace(name,'[^A-Za-z0-9]+','_','g'))),
    name=COALESCE(name,code), is_system_role=COALESCE(is_system_role,FALSE), is_active=COALESCE(is_active,TRUE);
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_roles_code ON public.admin_roles(code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_roles_name ON public.admin_roles(name);
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_roles_name_ci ON public.admin_roles(lower(name));
CREATE INDEX IF NOT EXISTS idx_admin_roles_active ON public.admin_roles(is_active);
ALTER TABLE public.admin_roles ALTER COLUMN code SET NOT NULL;
ALTER TABLE public.admin_roles ALTER COLUMN name SET NOT NULL;

CREATE TABLE IF NOT EXISTS public.admin_permissions (
    id BIGSERIAL PRIMARY KEY, code VARCHAR(100), name VARCHAR(150), module VARCHAR(100),
    description VARCHAR(500) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE public.admin_permissions ADD COLUMN IF NOT EXISTS code VARCHAR(100);
ALTER TABLE public.admin_permissions ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE public.admin_permissions ADD COLUMN IF NOT EXISTS module VARCHAR(100);
ALTER TABLE public.admin_permissions ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE public.admin_permissions ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.admin_permissions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_permissions' AND column_name='active') THEN
        UPDATE public.admin_permissions SET is_active=active WHERE active=FALSE AND is_active=TRUE;
    END IF;
END $$;
UPDATE public.admin_permissions SET code=COALESCE(code,name), name=COALESCE(name,code),
    module=COALESCE(module,split_part(COALESCE(code,name),'_',1)), description=COALESCE(description,name,code), is_active=COALESCE(is_active,TRUE);
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_permissions_code ON public.admin_permissions(code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_permissions_name ON public.admin_permissions(name);
CREATE INDEX IF NOT EXISTS idx_admin_permissions_module ON public.admin_permissions(module);
CREATE INDEX IF NOT EXISTS idx_admin_permissions_active ON public.admin_permissions(is_active);
ALTER TABLE public.admin_permissions ALTER COLUMN code SET NOT NULL;
ALTER TABLE public.admin_permissions ALTER COLUMN name SET NOT NULL;
ALTER TABLE public.admin_permissions ALTER COLUMN module SET NOT NULL;

DO $$ BEGIN
    IF to_regclass('public.admin_user_role_mapping') IS NULL AND to_regclass('public.admin_user_roles') IS NOT NULL THEN
        ALTER TABLE public.admin_user_roles RENAME TO admin_user_role_mapping;
    END IF;
    IF to_regclass('public.admin_role_permission_mapping') IS NULL AND to_regclass('public.admin_role_permissions') IS NOT NULL THEN
        ALTER TABLE public.admin_role_permissions RENAME TO admin_role_permission_mapping;
    END IF;
END $$;
CREATE TABLE IF NOT EXISTS public.admin_user_role_mapping (
    admin_user_id BIGINT NOT NULL REFERENCES public.admin_users(id) ON DELETE CASCADE,
    admin_role_id BIGINT NOT NULL REFERENCES public.admin_roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_user_id,admin_role_id)
);
CREATE TABLE IF NOT EXISTS public.admin_role_permission_mapping (
    admin_role_id BIGINT NOT NULL REFERENCES public.admin_roles(id) ON DELETE CASCADE,
    admin_permission_id BIGINT NOT NULL REFERENCES public.admin_permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_role_id,admin_permission_id)
);
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_user_role_mapping' AND column_name='role_id') THEN
        ALTER TABLE public.admin_user_role_mapping RENAME COLUMN role_id TO admin_role_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_role_permission_mapping' AND column_name='role_id') THEN
        ALTER TABLE public.admin_role_permission_mapping RENAME COLUMN role_id TO admin_role_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_role_permission_mapping' AND column_name='permission_id') THEN
        ALTER TABLE public.admin_role_permission_mapping RENAME COLUMN permission_id TO admin_permission_id;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_admin_user_role_mapping_role ON public.admin_user_role_mapping(admin_role_id);
CREATE INDEX IF NOT EXISTS idx_admin_role_permission_mapping_permission ON public.admin_role_permission_mapping(admin_permission_id);

CREATE TABLE IF NOT EXISTS public.admin_refresh_tokens (
    id BIGSERIAL PRIMARY KEY, admin_user_id BIGINT NOT NULL REFERENCES public.admin_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE, expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITHOUT TIME ZONE, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_admin_refresh_tokens_user ON public.admin_refresh_tokens(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_refresh_tokens_expires ON public.admin_refresh_tokens(expires_at);

CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
    id BIGSERIAL PRIMARY KEY, admin_user_id BIGINT REFERENCES public.admin_users(id) ON DELETE SET NULL,
    action VARCHAR(150) NOT NULL, module VARCHAR(100), entity_type VARCHAR(100), entity_id VARCHAR(100),
    description TEXT, ip_address VARCHAR(100), user_agent TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_user ON public.admin_audit_logs(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action ON public.admin_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_module ON public.admin_audit_logs(module);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_entity ON public.admin_audit_logs(entity_type,entity_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created ON public.admin_audit_logs(created_at DESC);

INSERT INTO public.admin_roles(code,name,description,is_system_role,is_active) VALUES
 ('SUPER_ADMIN','SUPER_ADMIN','Full Admin access.',TRUE,TRUE),
 ('ADMIN','ADMIN','Access only to explicitly assigned permissions.',TRUE,TRUE)
ON CONFLICT (code) DO NOTHING;
INSERT INTO public.admin_permissions(code,name,module,description,is_active) VALUES
 ('USER_VIEW','USER_VIEW','USER','View application users.',TRUE),
 ('USER_CREATE','USER_CREATE','USER','Create Admin accounts.',TRUE),
 ('USER_UPDATE','USER_UPDATE','USER','Update Admin accounts.',TRUE),
 ('USER_DISABLE','USER_DISABLE','USER','Disable Admin accounts.',TRUE),
 ('USER_ENABLE','USER_ENABLE','USER','Enable Admin accounts.',TRUE),
 ('ROLE_VIEW','ROLE_VIEW','ROLE','View Admin roles.',TRUE),
 ('ROLE_CREATE','ROLE_CREATE','ROLE','Create Admin roles.',TRUE),
 ('ROLE_UPDATE','ROLE_UPDATE','ROLE','Update Admin roles.',TRUE),
 ('ROLE_DELETE','ROLE_DELETE','ROLE','Delete Admin roles.',TRUE),
 ('ROLE_ASSIGN','ROLE_ASSIGN','ROLE','Assign roles to Admin users.',TRUE),
 ('PERMISSION_VIEW','PERMISSION_VIEW','PERMISSION','View Admin permissions.',TRUE),
 ('PERMISSION_ASSIGN','PERMISSION_ASSIGN','PERMISSION','Assign permissions to Admin roles.',TRUE),
 ('VERIFICATION_VIEW','VERIFICATION_VIEW','VERIFICATION','View verification requests.',TRUE),
 ('VERIFICATION_ASSIGN','VERIFICATION_ASSIGN','VERIFICATION','Assign verification requests.',TRUE),
 ('VERIFICATION_REVIEW','VERIFICATION_REVIEW','VERIFICATION','Review verification sections and evidence.',TRUE),
 ('VERIFICATION_APPROVE','VERIFICATION_APPROVE','VERIFICATION','Approve verifications and evidence.',TRUE),
 ('VERIFICATION_REQUEST_CHANGES','VERIFICATION_REQUEST_CHANGES','VERIFICATION','Request verification changes.',TRUE),
 ('VERIFICATION_REJECT','VERIFICATION_REJECT','VERIFICATION','Reject verification requests.',TRUE),
 ('AUDIT_LOG_VIEW','AUDIT_LOG_VIEW','AUDIT','View Admin audit logs.',TRUE),
 ('SYSTEM_SETTINGS_VIEW','SYSTEM_SETTINGS_VIEW','SYSTEM','View system settings.',TRUE),
 ('SYSTEM_SETTINGS_UPDATE','SYSTEM_SETTINGS_UPDATE','SYSTEM','Update system settings.',TRUE)
ON CONFLICT (code) DO NOTHING;
/* Existing Admin screens outside the requested identity/RBAC APIs keep their
   own explicit permission keys instead of inheriting broad default access. */
INSERT INTO public.admin_permissions(code,name,module,description,is_active) VALUES
 ('DASHBOARD_VIEW','DASHBOARD_VIEW','DASHBOARD','View Admin dashboards.',TRUE),
 ('CATALOG_VIEW','CATALOG_VIEW','CATALOG','Read Admin catalog data.',TRUE),
 ('CATALOG_MANAGE','CATALOG_MANAGE','CATALOG','Manage Admin catalog data.',TRUE),
 ('EVENT_REVIEW_VIEW','EVENT_REVIEW_VIEW','EVENT','View event review data.',TRUE),
 ('EVENT_REVIEW_MANAGE','EVENT_REVIEW_MANAGE','EVENT','Review and manage events.',TRUE),
 ('INSTITUTION_VIEW','INSTITUTION_VIEW','INSTITUTION','View institution data.',TRUE),
 ('INSTITUTION_IMPORT','INSTITUTION_IMPORT','INSTITUTION','Import institution data.',TRUE),
 ('INSTITUTION_MANAGE','INSTITUTION_MANAGE','INSTITUTION','Manage institutions.',TRUE),
 ('TECH_UPDATE_MANAGE','TECH_UPDATE_MANAGE','TECH_UPDATE','Manage technology updates.',TRUE),
 ('TRAINING_FINANCIAL_VIEW','TRAINING_FINANCIAL_VIEW','FINANCE','View training financial data.',TRUE),
 ('TRAINING_FINANCIAL_MANAGE','TRAINING_FINANCIAL_MANAGE','FINANCE','Manage training financial configuration.',TRUE),
 ('TRAINING_SETTLEMENT_PROCESS','TRAINING_SETTLEMENT_PROCESS','FINANCE','Process training settlements.',TRUE),
 ('TRAINING_SETTLEMENT_RECONCILE','TRAINING_SETTLEMENT_RECONCILE','FINANCE','Refresh trainer settlement payment status.',TRUE)
ON CONFLICT (code) DO NOTHING;
INSERT INTO public.admin_role_permission_mapping(admin_role_id,admin_permission_id)
SELECT r.id,p.id FROM public.admin_roles r CROSS JOIN public.admin_permissions p
WHERE r.code='SUPER_ADMIN' AND p.is_active=TRUE ON CONFLICT DO NOTHING;
/* Retain legacy assignments for auditability, but make the obsolete role inert. */
UPDATE public.admin_roles SET is_active=FALSE,updated_at=CURRENT_TIMESTAMP WHERE code='NORMAL_USER';

/* Event reviewer identity is Admin-only too. Preserve legacy rows as unvalidated
   while requiring Admin identities for all subsequent reviewer writes. */
DO $$
DECLARE c RECORD;
BEGIN
    IF to_regclass('public.events') IS NULL OR to_regclass('public.users') IS NULL
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='events' AND column_name='reviewed_by_user_id') THEN RETURN; END IF;
    FOR c IN SELECT conname FROM pg_constraint
        WHERE conrelid='public.events'::regclass AND contype='f'
          AND confrelid='public.users'::regclass AND pg_get_constraintdef(oid) LIKE '%reviewed_by_user_id%'
    LOOP EXECUTE format('ALTER TABLE public.events DROP CONSTRAINT %I',c.conname); END LOOP;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint event_constraint
        JOIN pg_attribute event_attribute ON event_attribute.attrelid=event_constraint.conrelid AND event_attribute.attnum=ANY(event_constraint.conkey)
        WHERE event_constraint.conrelid='public.events'::regclass AND event_constraint.contype='f'
          AND event_constraint.confrelid='public.admin_users'::regclass AND event_attribute.attname='reviewed_by_user_id'
    ) THEN
        ALTER TABLE public.events ADD CONSTRAINT fk_events_admin_reviewer
            FOREIGN KEY (reviewed_by_user_id) REFERENCES public.admin_users(id) ON DELETE SET NULL NOT VALID;
    END IF;
END $$;

COMMIT;
