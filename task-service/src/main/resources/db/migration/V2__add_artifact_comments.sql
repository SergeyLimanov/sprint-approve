-- Add artifact_id column to comments table to support comments on artifacts
-- Comments can now be attached to either a task or an artifact

ALTER TABLE comments 
    ALTER COLUMN task_id DROP NOT NULL;

ALTER TABLE comments 
    ADD COLUMN artifact_id BIGINT;

ALTER TABLE comments 
    ADD CONSTRAINT fk_comments_artifact 
    FOREIGN KEY (artifact_id) 
    REFERENCES artifacts(id) 
    ON DELETE CASCADE;

-- Add check constraint to ensure comment is attached to either task or artifact
ALTER TABLE comments 
    ADD CONSTRAINT check_comment_target 
    CHECK (
        (task_id IS NOT NULL AND artifact_id IS NULL) OR 
        (task_id IS NULL AND artifact_id IS NOT NULL)
    );

-- Create index for faster artifact comment queries
CREATE INDEX idx_comments_artifact_id ON comments(artifact_id);
