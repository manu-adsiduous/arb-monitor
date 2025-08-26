-- Migration V2: Add missing tables and relationships

-- Scraped ad image URLs (many-to-many relationship)
CREATE TABLE IF NOT EXISTS scraped_ad_image_urls (
    ad_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    PRIMARY KEY (ad_id, image_url),
    FOREIGN KEY (ad_id) REFERENCES scraped_ads(id) ON DELETE CASCADE
);

-- Scraped ad video URLs (many-to-many relationship)  
CREATE TABLE IF NOT EXISTS scraped_ad_video_urls (
    ad_id BIGINT NOT NULL,
    video_url VARCHAR(1000) NOT NULL,
    PRIMARY KEY (ad_id, video_url),
    FOREIGN KEY (ad_id) REFERENCES scraped_ads(id) ON DELETE CASCADE
);

-- Scraped ad local image paths (many-to-many relationship)
CREATE TABLE IF NOT EXISTS scraped_ad_local_images (
    ad_id BIGINT NOT NULL,
    local_image_path VARCHAR(1000) NOT NULL,
    PRIMARY KEY (ad_id, local_image_path),
    FOREIGN KEY (ad_id) REFERENCES scraped_ads(id) ON DELETE CASCADE
);

-- Scraped ad local video paths (many-to-many relationship)
CREATE TABLE IF NOT EXISTS scraped_ad_local_videos (
    ad_id BIGINT NOT NULL,
    local_video_path VARCHAR(1000) NOT NULL,
    PRIMARY KEY (ad_id, local_video_path),
    FOREIGN KEY (ad_id) REFERENCES scraped_ads(id) ON DELETE CASCADE
);

-- Add extracted text fields to scraped_ads if not exists
ALTER TABLE scraped_ads ADD COLUMN IF NOT EXISTS extracted_text TEXT;
ALTER TABLE scraped_ads ADD COLUMN IF NOT EXISTS extracted_audio_text TEXT;

