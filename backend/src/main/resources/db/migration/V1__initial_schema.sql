-- Every business relationship carries organization_id. Composite foreign keys prevent
-- cross-workspace references even when application-level validation is accidentally missed.
CREATE TABLE organization (
 id uuid PRIMARY KEY, name varchar(160) NOT NULL, timezone varchar(60) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app_user (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization(id),
 email varchar(254) NOT NULL UNIQUE CHECK (email = lower(email)), display_name varchar(120) NOT NULL,
 password_hash varchar(100) NOT NULL, role varchar(20) NOT NULL CHECK(role IN ('ADMIN','SUPERVISOR','INSPECTOR','TECHNICIAN','VIEWER')),
 active boolean NOT NULL DEFAULT true, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(organization_id,id)
);
CREATE TABLE site (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization(id),
 name varchar(160) NOT NULL, code varchar(32) NOT NULL, address varchar(400) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(organization_id,id), UNIQUE(organization_id,code)
);
CREATE TABLE asset (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization(id), site_id uuid NOT NULL,
 code varchar(40) NOT NULL, name varchar(160) NOT NULL, category varchar(80) NOT NULL,
 manufacturer varchar(120) NOT NULL DEFAULT '', model varchar(120) NOT NULL DEFAULT '',
 serial_number varchar(120) NOT NULL DEFAULT '', location_note varchar(240) NOT NULL DEFAULT '',
 criticality varchar(12) NOT NULL CHECK(criticality IN ('LOW','MEDIUM','HIGH','CRITICAL')),
 archived boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(organization_id,id), UNIQUE(organization_id,code),
 FOREIGN KEY (organization_id,site_id) REFERENCES site(organization_id,id)
);
CREATE TABLE inspection_template (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization(id),
 name varchar(160) NOT NULL, name_de varchar(160) NOT NULL, description varchar(2000) NOT NULL DEFAULT '',
 version integer NOT NULL CHECK(version > 0), status varchar(12) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','REVIEWED')),
 reviewed_by uuid, reviewed_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(organization_id,id), FOREIGN KEY(organization_id,reviewed_by) REFERENCES app_user(organization_id,id)
);
CREATE TABLE template_item (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, template_id uuid NOT NULL,
 position integer NOT NULL, label varchar(400) NOT NULL, label_de varchar(400) NOT NULL,
 kind varchar(12) NOT NULL CHECK(kind IN ('PASS_FAIL','NUMBER','TEXT')), required boolean NOT NULL,
 min_value numeric(18,6), max_value numeric(18,6), unit varchar(24) NOT NULL DEFAULT '',
 UNIQUE(organization_id,id), UNIQUE(template_id,position),
 CHECK(min_value IS NULL OR max_value IS NULL OR min_value <= max_value),
 FOREIGN KEY(organization_id,template_id) REFERENCES inspection_template(organization_id,id)
);
CREATE TABLE inspection_schedule (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, asset_id uuid NOT NULL, template_id uuid NOT NULL, owner_id uuid NOT NULL,
 interval_days integer NOT NULL CHECK(interval_days BETWEEN 1 AND 3650), next_due date NOT NULL,
 reason varchar(2000) NOT NULL, reviewed_by uuid NOT NULL, active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(organization_id,id),
 FOREIGN KEY(organization_id,asset_id) REFERENCES asset(organization_id,id),
 FOREIGN KEY(organization_id,template_id) REFERENCES inspection_template(organization_id,id),
 FOREIGN KEY(organization_id,owner_id) REFERENCES app_user(organization_id,id),
 FOREIGN KEY(organization_id,reviewed_by) REFERENCES app_user(organization_id,id)
);
CREATE UNIQUE INDEX one_active_schedule ON inspection_schedule(organization_id,asset_id,template_id) WHERE active;
CREATE INDEX schedules_due ON inspection_schedule(organization_id,next_due) WHERE active;
CREATE TABLE inspection (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, asset_id uuid NOT NULL, template_id uuid NOT NULL,
 schedule_id uuid, inspector_id uuid NOT NULL, request_key uuid NOT NULL,
 status varchar(12) NOT NULL CHECK(status IN ('DRAFT','SUBMITTED','VERIFIED','REJECTED')),
 planned_due date, performed_at timestamptz, submitted_at timestamptz, reviewed_by uuid,
 review_note varchar(2000) NOT NULL DEFAULT '', version integer NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(organization_id,id), UNIQUE(organization_id,request_key),
 FOREIGN KEY(organization_id,asset_id) REFERENCES asset(organization_id,id),
 FOREIGN KEY(organization_id,template_id) REFERENCES inspection_template(organization_id,id),
 FOREIGN KEY(organization_id,schedule_id) REFERENCES inspection_schedule(organization_id,id),
 FOREIGN KEY(organization_id,inspector_id) REFERENCES app_user(organization_id,id),
 FOREIGN KEY(organization_id,reviewed_by) REFERENCES app_user(organization_id,id)
);
CREATE UNIQUE INDEX one_running_inspection ON inspection(organization_id,schedule_id) WHERE schedule_id IS NOT NULL AND status IN ('DRAFT','SUBMITTED');
CREATE INDEX inspection_asset ON inspection(organization_id,asset_id,created_at DESC);
CREATE TABLE inspection_answer (
 organization_id uuid NOT NULL, inspection_id uuid NOT NULL, item_id uuid NOT NULL,
 value varchar(2000) NOT NULL DEFAULT '', note varchar(2000) NOT NULL DEFAULT '',
 outcome varchar(8) NOT NULL CHECK(outcome IN ('PASS','FAIL','NA')), PRIMARY KEY(inspection_id,item_id),
 FOREIGN KEY(organization_id,inspection_id) REFERENCES inspection(organization_id,id),
 FOREIGN KEY(organization_id,item_id) REFERENCES template_item(organization_id,id)
);
CREATE TABLE finding (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, asset_id uuid NOT NULL, inspection_id uuid NOT NULL,
 item_id uuid NOT NULL, title varchar(400) NOT NULL, description varchar(2000) NOT NULL DEFAULT '',
 severity varchar(12) NOT NULL CHECK(severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
 assignee_id uuid, due_date date, status varchar(24) NOT NULL CHECK(status IN ('OPEN','IN_PROGRESS','AWAITING_VERIFICATION','CLOSED')),
 repair_note varchar(4000) NOT NULL DEFAULT '', completed_by uuid, verified_by uuid,
 verify_note varchar(2000) NOT NULL DEFAULT '', resolved_at timestamptz, version integer NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(organization_id,id), UNIQUE(inspection_id,item_id),
 FOREIGN KEY(organization_id,asset_id) REFERENCES asset(organization_id,id),
 FOREIGN KEY(organization_id,inspection_id) REFERENCES inspection(organization_id,id),
 FOREIGN KEY(organization_id,item_id) REFERENCES template_item(organization_id,id),
 FOREIGN KEY(organization_id,assignee_id) REFERENCES app_user(organization_id,id),
 FOREIGN KEY(organization_id,completed_by) REFERENCES app_user(organization_id,id),
 FOREIGN KEY(organization_id,verified_by) REFERENCES app_user(organization_id,id)
);
CREATE INDEX finding_queue ON finding(organization_id,status,due_date);
CREATE TABLE evidence (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, asset_id uuid NOT NULL, inspection_id uuid, finding_id uuid,
 uploaded_by uuid NOT NULL, filename varchar(180) NOT NULL, mime varchar(80) NOT NULL,
 size bigint NOT NULL CHECK(size BETWEEN 1 AND 10485760), sha256 char(64) NOT NULL,
 data bytea NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(organization_id,id),
 CHECK(inspection_id IS NOT NULL OR finding_id IS NOT NULL),
 FOREIGN KEY(organization_id,asset_id) REFERENCES asset(organization_id,id),
 FOREIGN KEY(organization_id,inspection_id) REFERENCES inspection(organization_id,id),
 FOREIGN KEY(organization_id,finding_id) REFERENCES finding(organization_id,id),
 FOREIGN KEY(organization_id,uploaded_by) REFERENCES app_user(organization_id,id)
);
CREATE INDEX evidence_asset ON evidence(organization_id,asset_id,created_at DESC);
CREATE TABLE audit_event (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization(id), actor_id uuid,
 entity_type varchar(40) NOT NULL, entity_id uuid NOT NULL, action varchar(60) NOT NULL,
 detail varchar(4000) NOT NULL DEFAULT '', occurred_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(organization_id,actor_id) REFERENCES app_user(organization_id,id)
);
CREATE INDEX audit_timeline ON audit_event(organization_id,occurred_at DESC);
CREATE FUNCTION deny_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Audit events are append-only'; END; $$;
CREATE TRIGGER immutable_audit BEFORE UPDATE OR DELETE ON audit_event FOR EACH ROW EXECUTE FUNCTION deny_audit_mutation();
