package com.project.smart_city_tracker_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Uploads a file to Cloudinary.
     *
     * @param file The MultipartFile to upload.
     * @return A Map containing the secure URL and the public ID of the uploaded file.
     * @throws IOException If an error occurs during the upload process.
     */
    public Map<String, String> uploadFile(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
            ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "smart_city_issues"
            ));

        String url = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        return Map.of("url", url, "publicId", publicId);
    }
}