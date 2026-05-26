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

CREATE TABLE IF NOT EXISTS enterprise_categories (
    id uuid PRIMARY KEY,
    sector varchar(200) NOT NULL,
    category_group varchar(200) NOT NULL,
    item_label varchar(300) NOT NULL,
    keywords varchar(1000),
    created_at timestamp,
    UNIQUE (sector, category_group, item_label)
);

UPDATE listings SET status = 'PARTNER_INACTIVE' WHERE partner_id IS NOT NULL AND status = 'PARTNER_PENDING_APPROVAL';
UPDATE listings SET status = 'PARTNER_ACTIVE' WHERE partner_id IS NOT NULL AND status = 'APPROVED';
