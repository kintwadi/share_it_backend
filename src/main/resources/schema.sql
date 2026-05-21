ALTER TABLE listings ADD COLUMN available_unlimited boolean DEFAULT true;
ALTER TABLE listings ADD COLUMN available_from timestamp;
ALTER TABLE listings ADD COLUMN available_to timestamp;
ALTER TABLE listings ADD COLUMN partner_submitted_at timestamp;
ALTER TABLE listings ADD COLUMN partner_submitted_by uuid;
ALTER TABLE listings ADD COLUMN partner_reviewed_at timestamp;
ALTER TABLE listings ADD COLUMN partner_reviewed_by uuid;
ALTER TABLE listings ADD COLUMN partner_review_note varchar(500);
ALTER TABLE listings ADD COLUMN partner_rejection_reason varchar(500);
