ALTER TABLE listings ADD COLUMN IF NOT EXISTS available_unlimited boolean DEFAULT true;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS available_from timestamp;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS available_to timestamp;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_submitted_at timestamp;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_submitted_by uuid;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_reviewed_at timestamp;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_reviewed_by uuid;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_review_note varchar(500);
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_rejection_reason varchar(500);
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_borrow_requested_at timestamp;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_borrow_requested_by uuid;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_borrow_reviewed_at timestamp;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_borrow_reviewed_by uuid;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS partner_borrow_rejection_reason varchar(500);
ALTER TABLE listings ADD COLUMN IF NOT EXISTS item_reference varchar(8);
ALTER TABLE users ADD COLUMN IF NOT EXISTS admin_scope varchar(20);
ALTER TABLE pickup_locations ADD COLUMN IF NOT EXISTS reference_id varchar(32);
ALTER TABLE pickup_locations ADD COLUMN IF NOT EXISTS street_address varchar(255);
ALTER TABLE pickup_locations ADD COLUMN IF NOT EXISTS city varchar(255);
ALTER TABLE pickup_locations ADD COLUMN IF NOT EXISTS postal_code varchar(40);
ALTER TABLE pickup_locations ADD COLUMN IF NOT EXISTS country varchar(80);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pickup_locations_reference_id ON pickup_locations (reference_id);

UPDATE listings SET status = 'PARTNER_INACTIVE' WHERE partner_id IS NOT NULL AND status = 'PARTNER_PENDING_APPROVAL';
UPDATE listings SET status = 'PARTNER_ACTIVE' WHERE partner_id IS NOT NULL AND status = 'APPROVED';
