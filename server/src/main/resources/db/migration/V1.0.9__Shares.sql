CREATE TABLE shares (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    creatorUserId VARCHAR(255) NOT NULL,
    resourceType  VARCHAR(50)  NOT NULL,
    resourceId    UUID         NOT NULL,
    expiresAt     TIMESTAMPTZ,
    revokedAt     TIMESTAMPTZ,
    createdAt     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_shares_resource ON shares (resourceType, resourceId);
CREATE INDEX idx_shares_creator  ON shares (creatorUserId);
