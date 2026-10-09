--
-- PostgreSQL database dump
--

-- Dumped from database version 15.3
-- Dumped by pg_dump version 15.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: administrative_areas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.administrative_areas (
    id bigint NOT NULL,
    country_id bigint NOT NULL,
    code character varying(100),
    name character varying(200) NOT NULL,
    area_type character varying(50),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone
);


--
-- Name: administrative_areas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.administrative_areas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: administrative_areas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.administrative_areas_id_seq OWNED BY public.administrative_areas.id;


--
-- Name: cities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cities (
    id bigint NOT NULL,
    country_id bigint NOT NULL,
    administrative_area_id bigint,
    name character varying(200) NOT NULL,
    latitude numeric(10,7),
    longitude numeric(10,7),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone
);


--
-- Name: cities_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cities_id_seq OWNED BY public.cities.id;


--
-- Name: countries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.countries (
    id bigint NOT NULL,
    iso_code character varying(10) NOT NULL,
    iso3_code character varying(10),
    name character varying(150) NOT NULL,
    phone_code character varying(20),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone
);


--
-- Name: countries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.countries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: countries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.countries_id_seq OWNED BY public.countries.id;


--
-- Name: institution_aliases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_aliases (
    id bigint NOT NULL,
    institution_id bigint NOT NULL,
    alias_name character varying(255) NOT NULL,
    normalized_alias_name character varying(500) NOT NULL,
    alias_type character varying(50),
    source_id bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    normalized_alias character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: institution_aliases_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_aliases_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_aliases_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_aliases_id_seq OWNED BY public.institution_aliases.id;


--
-- Name: institution_import_record_matches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_import_record_matches (
    id bigint NOT NULL,
    import_record_id bigint NOT NULL,
    institution_id bigint NOT NULL,
    match_score integer NOT NULL,
    match_reason jsonb,
    resolution character varying(50) DEFAULT 'PENDING'::character varying,
    resolved_by character varying(100),
    resolved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    match_confidence character varying(50) NOT NULL,
    matched_institution_id bigint NOT NULL,
    resolution_status character varying(50) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT institution_import_record_matches_match_confidence_check CHECK (((match_confidence)::text = ANY ((ARRAY['HIGH_CONFIDENCE'::character varying, 'POSSIBLE_DUPLICATE'::character varying, 'NO_DUPLICATE'::character varying])::text[]))),
    CONSTRAINT institution_import_record_matches_resolution_status_check CHECK (((resolution_status)::text = ANY ((ARRAY['PENDING'::character varying, 'MERGED'::character varying, 'KEPT_SEPARATE'::character varying])::text[])))
);


--
-- Name: institution_import_record_matches_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_import_record_matches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_import_record_matches_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_import_record_matches_id_seq OWNED BY public.institution_import_record_matches.id;


--
-- Name: institution_import_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_import_records (
    id bigint NOT NULL,
    import_id bigint NOT NULL,
    row_number integer,
    source_identifier character varying(150),
    raw_data jsonb,
    normalized_data jsonb,
    action character varying(50),
    processing_status character varying(50) DEFAULT 'PENDING'::character varying NOT NULL,
    institution_id bigint,
    match_confidence numeric(5,2),
    error_message text,
    processed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    classification character varying(50),
    mapped_data jsonb,
    row_index integer NOT NULL,
    target_institution_id bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    validation_errors jsonb,
    validation_status character varying(50) NOT NULL,
    CONSTRAINT institution_import_records_classification_check CHECK (((classification)::text = ANY ((ARRAY['NEW'::character varying, 'UPDATE'::character varying, 'UNCHANGED'::character varying, 'DUPLICATE'::character varying, 'ERROR'::character varying])::text[]))),
    CONSTRAINT institution_import_records_validation_status_check CHECK (((validation_status)::text = ANY ((ARRAY['VALID'::character varying, 'INVALID'::character varying])::text[])))
);


--
-- Name: institution_import_records_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_import_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_import_records_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_import_records_id_seq OWNED BY public.institution_import_records.id;


--
-- Name: institution_imports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_imports (
    id bigint NOT NULL,
    source_id bigint NOT NULL,
    country_id bigint NOT NULL,
    version character varying(50),
    file_name character varying(255),
    file_hash character varying(128),
    import_type character varying(50) DEFAULT 'FULL'::character varying NOT NULL,
    status character varying(50) DEFAULT 'CREATED'::character varying NOT NULL,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    total_records integer DEFAULT 0 NOT NULL,
    valid_records integer DEFAULT 0 NOT NULL,
    inserted_count integer DEFAULT 0 NOT NULL,
    updated_count integer DEFAULT 0 NOT NULL,
    unchanged_count integer DEFAULT 0 NOT NULL,
    duplicate_count integer DEFAULT 0 NOT NULL,
    error_count integer DEFAULT 0 NOT NULL,
    created_by character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    error_message character varying(2000),
    file_path character varying(500),
    new_count integer NOT NULL,
    summary jsonb,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: institution_imports_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_imports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_imports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_imports_id_seq OWNED BY public.institution_imports.id;


--
-- Name: institution_source_mappings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_source_mappings (
    id bigint NOT NULL,
    source_id bigint NOT NULL,
    source_field_name character varying(150) NOT NULL,
    standard_field_code character varying(100) NOT NULL,
    transformation_rule character varying(50),
    transformation_config jsonb,
    required boolean DEFAULT false NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    default_value character varying(255)
);


--
-- Name: institution_source_mappings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_source_mappings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_source_mappings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_source_mappings_id_seq OWNED BY public.institution_source_mappings.id;


--
-- Name: institution_source_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_source_records (
    id bigint NOT NULL,
    institution_id bigint NOT NULL,
    source_id bigint NOT NULL,
    source_identifier character varying(150) NOT NULL,
    source_name character varying(500),
    source_data jsonb,
    first_seen_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_seen_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_import_id bigint,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    raw_data jsonb,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: institution_source_records_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_source_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_source_records_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_source_records_id_seq OWNED BY public.institution_source_records.id;


--
-- Name: institution_sources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_sources (
    id bigint NOT NULL,
    country_id bigint NOT NULL,
    source_code character varying(100) NOT NULL,
    source_name character varying(200) NOT NULL,
    source_type character varying(50) NOT NULL,
    source_url character varying(1000),
    description character varying(500),
    import_method character varying(50) DEFAULT 'FILE_UPLOAD'::character varying NOT NULL,
    active boolean DEFAULT true NOT NULL,
    priority integer DEFAULT 1 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone
);


--
-- Name: institution_sources_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_sources_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_sources_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_sources_id_seq OWNED BY public.institution_sources.id;


--
-- Name: institution_standard_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_standard_fields (
    id bigint NOT NULL,
    field_code character varying(100) NOT NULL,
    field_name character varying(200) NOT NULL,
    data_type character varying(50) NOT NULL,
    required boolean DEFAULT false NOT NULL,
    description character varying(255),
    active boolean DEFAULT true NOT NULL,
    display_order integer DEFAULT 0 NOT NULL
);


--
-- Name: institution_standard_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_standard_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_standard_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_standard_fields_id_seq OWNED BY public.institution_standard_fields.id;


--
-- Name: institutions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institutions (
    id bigint NOT NULL,
    global_identifier character varying(150),
    name character varying(255) NOT NULL,
    normalized_name character varying(255) NOT NULL,
    short_name character varying(100),
    institution_type character varying(100),
    country_id bigint NOT NULL,
    administrative_area_id bigint,
    city_id bigint,
    address character varying(500),
    website character varying(500),
    verification_status character varying(50) DEFAULT 'VERIFIED'::character varying NOT NULL,
    source_id bigint,
    source_identifier character varying(250),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    city_name character varying(100),
    normalized_website character varying(500),
    state_id bigint
);


--
-- Name: institutions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institutions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institutions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institutions_id_seq OWNED BY public.institutions.id;


--
-- Name: administrative_areas id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrative_areas ALTER COLUMN id SET DEFAULT nextval('public.administrative_areas_id_seq'::regclass);


--
-- Name: cities id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cities ALTER COLUMN id SET DEFAULT nextval('public.cities_id_seq'::regclass);


--
-- Name: countries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries ALTER COLUMN id SET DEFAULT nextval('public.countries_id_seq'::regclass);


--
-- Name: institution_aliases id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_aliases ALTER COLUMN id SET DEFAULT nextval('public.institution_aliases_id_seq'::regclass);


--
-- Name: institution_import_record_matches id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_record_matches ALTER COLUMN id SET DEFAULT nextval('public.institution_import_record_matches_id_seq'::regclass);


--
-- Name: institution_import_records id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_records ALTER COLUMN id SET DEFAULT nextval('public.institution_import_records_id_seq'::regclass);


--
-- Name: institution_imports id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_imports ALTER COLUMN id SET DEFAULT nextval('public.institution_imports_id_seq'::regclass);


--
-- Name: institution_source_mappings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_mappings ALTER COLUMN id SET DEFAULT nextval('public.institution_source_mappings_id_seq'::regclass);


--
-- Name: institution_source_records id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_records ALTER COLUMN id SET DEFAULT nextval('public.institution_source_records_id_seq'::regclass);


--
-- Name: institution_sources id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_sources ALTER COLUMN id SET DEFAULT nextval('public.institution_sources_id_seq'::regclass);


--
-- Name: institution_standard_fields id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_standard_fields ALTER COLUMN id SET DEFAULT nextval('public.institution_standard_fields_id_seq'::regclass);


--
-- Name: institutions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions ALTER COLUMN id SET DEFAULT nextval('public.institutions_id_seq'::regclass);


--
-- Name: administrative_areas administrative_areas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrative_areas
    ADD CONSTRAINT administrative_areas_pkey PRIMARY KEY (id);


--
-- Name: cities cities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT cities_pkey PRIMARY KEY (id);


--
-- Name: countries countries_iso_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_iso_code_key UNIQUE (iso_code);


--
-- Name: countries countries_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_name_key UNIQUE (name);


--
-- Name: countries countries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_pkey PRIMARY KEY (id);


--
-- Name: institution_aliases institution_aliases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_aliases
    ADD CONSTRAINT institution_aliases_pkey PRIMARY KEY (id);


--
-- Name: institution_import_record_matches institution_import_record_matches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_record_matches
    ADD CONSTRAINT institution_import_record_matches_pkey PRIMARY KEY (id);


--
-- Name: institution_import_records institution_import_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_records
    ADD CONSTRAINT institution_import_records_pkey PRIMARY KEY (id);


--
-- Name: institution_imports institution_imports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_imports
    ADD CONSTRAINT institution_imports_pkey PRIMARY KEY (id);


--
-- Name: institution_source_mappings institution_source_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_mappings
    ADD CONSTRAINT institution_source_mappings_pkey PRIMARY KEY (id);


--
-- Name: institution_source_records institution_source_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_records
    ADD CONSTRAINT institution_source_records_pkey PRIMARY KEY (id);


--
-- Name: institution_sources institution_sources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_sources
    ADD CONSTRAINT institution_sources_pkey PRIMARY KEY (id);


--
-- Name: institution_standard_fields institution_standard_fields_field_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_standard_fields
    ADD CONSTRAINT institution_standard_fields_field_code_key UNIQUE (field_code);


--
-- Name: institution_standard_fields institution_standard_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_standard_fields
    ADD CONSTRAINT institution_standard_fields_pkey PRIMARY KEY (id);


--
-- Name: institutions institutions_global_identifier_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_global_identifier_key UNIQUE (global_identifier);


--
-- Name: institutions institutions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_pkey PRIMARY KEY (id);


--
-- Name: institution_source_records uk_inst_src_rec_source_ident; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_records
    ADD CONSTRAINT uk_inst_src_rec_source_ident UNIQUE (source_id, source_identifier);


--
-- Name: administrative_areas uq_admin_area_country_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrative_areas
    ADD CONSTRAINT uq_admin_area_country_name UNIQUE (country_id, name);


--
-- Name: institution_sources uq_institution_source_country_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_sources
    ADD CONSTRAINT uq_institution_source_country_code UNIQUE (country_id, source_code);


--
-- Name: institution_source_mappings uq_source_mapping; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_mappings
    ADD CONSTRAINT uq_source_mapping UNIQUE (source_id, source_field_name);


--
-- Name: institution_source_records uq_source_record; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_records
    ADD CONSTRAINT uq_source_record UNIQUE (source_id, source_identifier);


--
-- Name: idx_admin_area_country; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_area_country ON public.administrative_areas USING btree (country_id);


--
-- Name: idx_admin_area_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_area_name ON public.administrative_areas USING btree (name);


--
-- Name: idx_city_admin_area; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_city_admin_area ON public.cities USING btree (administrative_area_id);


--
-- Name: idx_city_country; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_city_country ON public.cities USING btree (country_id);


--
-- Name: idx_city_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_city_name ON public.cities USING btree (name);


--
-- Name: idx_countries_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_countries_active ON public.countries USING btree (active);


--
-- Name: idx_import_country; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_country ON public.institution_imports USING btree (country_id);


--
-- Name: idx_import_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_created ON public.institution_imports USING btree (created_at DESC);


--
-- Name: idx_import_record_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_record_action ON public.institution_import_records USING btree (action);


--
-- Name: idx_import_record_import; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_record_import ON public.institution_import_records USING btree (import_id);


--
-- Name: idx_import_record_source_identifier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_record_source_identifier ON public.institution_import_records USING btree (source_identifier);


--
-- Name: idx_import_record_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_record_status ON public.institution_import_records USING btree (processing_status);


--
-- Name: idx_import_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_source ON public.institution_imports USING btree (source_id);


--
-- Name: idx_import_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_status ON public.institution_imports USING btree (status);


--
-- Name: idx_institution_alias_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_alias_institution ON public.institution_aliases USING btree (institution_id);


--
-- Name: idx_institution_alias_normalized; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_alias_normalized ON public.institution_aliases USING btree (normalized_alias_name);


--
-- Name: idx_institution_city; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_city ON public.institutions USING btree (city_id);


--
-- Name: idx_institution_country; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_country ON public.institutions USING btree (country_id);


--
-- Name: idx_institution_normalized_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_normalized_name ON public.institutions USING btree (normalized_name);


--
-- Name: idx_institution_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_source ON public.institutions USING btree (source_id);


--
-- Name: idx_institution_source_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_source_active ON public.institution_sources USING btree (active);


--
-- Name: idx_institution_source_country; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_source_country ON public.institution_sources USING btree (country_id);


--
-- Name: idx_institution_source_identifier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_source_identifier ON public.institutions USING btree (source_identifier);


--
-- Name: idx_institution_verification; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_verification ON public.institutions USING btree (verification_status);


--
-- Name: idx_match_import_record; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_match_import_record ON public.institution_import_record_matches USING btree (import_record_id);


--
-- Name: idx_match_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_match_institution ON public.institution_import_record_matches USING btree (institution_id);


--
-- Name: idx_match_resolution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_match_resolution ON public.institution_import_record_matches USING btree (resolution);


--
-- Name: idx_source_mapping_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_source_mapping_source ON public.institution_source_mappings USING btree (source_id);


--
-- Name: idx_source_record_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_source_record_institution ON public.institution_source_records USING btree (institution_id);


--
-- Name: idx_source_record_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_source_record_source ON public.institution_source_records USING btree (source_id);


--
-- Name: uq_institution_country_normalized_name_city; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_institution_country_normalized_name_city ON public.institutions USING btree (country_id, normalized_name, city_id) WHERE (city_id IS NOT NULL);


--
-- Name: uq_institution_source_identifier; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_institution_source_identifier ON public.institutions USING btree (source_id, source_identifier) WHERE ((source_id IS NOT NULL) AND (source_identifier IS NOT NULL));


--
-- Name: institution_imports fk7jxao1w5nblq6vd7uen2apbr4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_imports
    ADD CONSTRAINT fk7jxao1w5nblq6vd7uen2apbr4 FOREIGN KEY (country_id) REFERENCES public.country(id);


--
-- Name: administrative_areas fk_admin_area_country; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrative_areas
    ADD CONSTRAINT fk_admin_area_country FOREIGN KEY (country_id) REFERENCES public.countries(id);


--
-- Name: institution_aliases fk_alias_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_aliases
    ADD CONSTRAINT fk_alias_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id) ON DELETE CASCADE;


--
-- Name: institution_aliases fk_alias_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_aliases
    ADD CONSTRAINT fk_alias_source FOREIGN KEY (source_id) REFERENCES public.institution_sources(id);


--
-- Name: cities fk_city_admin_area; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT fk_city_admin_area FOREIGN KEY (administrative_area_id) REFERENCES public.administrative_areas(id);


--
-- Name: cities fk_city_country; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT fk_city_country FOREIGN KEY (country_id) REFERENCES public.countries(id);


--
-- Name: institution_imports fk_import_country; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_imports
    ADD CONSTRAINT fk_import_country FOREIGN KEY (country_id) REFERENCES public.countries(id);


--
-- Name: institution_import_records fk_import_record_import; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_records
    ADD CONSTRAINT fk_import_record_import FOREIGN KEY (import_id) REFERENCES public.institution_imports(id) ON DELETE CASCADE;


--
-- Name: institution_import_records fk_import_record_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_records
    ADD CONSTRAINT fk_import_record_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: institution_imports fk_import_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_imports
    ADD CONSTRAINT fk_import_source FOREIGN KEY (source_id) REFERENCES public.institution_sources(id);


--
-- Name: institutions fk_institution_admin_area; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT fk_institution_admin_area FOREIGN KEY (administrative_area_id) REFERENCES public.administrative_areas(id);


--
-- Name: institutions fk_institution_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT fk_institution_city FOREIGN KEY (city_id) REFERENCES public.cities(id);


--
-- Name: institutions fk_institution_country; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT fk_institution_country FOREIGN KEY (country_id) REFERENCES public.countries(id);


--
-- Name: institutions fk_institution_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT fk_institution_source FOREIGN KEY (source_id) REFERENCES public.institution_sources(id);


--
-- Name: institution_sources fk_institution_source_country; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_sources
    ADD CONSTRAINT fk_institution_source_country FOREIGN KEY (country_id) REFERENCES public.countries(id);


--
-- Name: institution_source_mappings fk_mapping_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_mappings
    ADD CONSTRAINT fk_mapping_source FOREIGN KEY (source_id) REFERENCES public.institution_sources(id);


--
-- Name: institution_source_mappings fk_mapping_standard_field; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_mappings
    ADD CONSTRAINT fk_mapping_standard_field FOREIGN KEY (standard_field_code) REFERENCES public.institution_standard_fields(field_code);


--
-- Name: institution_import_record_matches fk_match_import_record; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_record_matches
    ADD CONSTRAINT fk_match_import_record FOREIGN KEY (import_record_id) REFERENCES public.institution_import_records(id) ON DELETE CASCADE;


--
-- Name: institution_import_record_matches fk_match_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_record_matches
    ADD CONSTRAINT fk_match_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: institution_source_records fk_source_record_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_records
    ADD CONSTRAINT fk_source_record_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: institution_source_records fk_source_record_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_source_records
    ADD CONSTRAINT fk_source_record_source FOREIGN KEY (source_id) REFERENCES public.institution_sources(id);


--
-- Name: institution_import_record_matches fkpshwoqf599w3mrbrms072b6nx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_import_record_matches
    ADD CONSTRAINT fkpshwoqf599w3mrbrms072b6nx FOREIGN KEY (matched_institution_id) REFERENCES public.institutions(id);


--
-- PostgreSQL database dump complete
--

