package uk.gov.hmcts.reform.wapostdeploymentfttests.preparers;

import com.google.common.io.ByteStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.document.utils.InMemoryMultipartFile;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.documents.Document;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

import java.io.IOException;
import java.util.Collections;

import static java.lang.String.format;

@Service
public class DocumentManagementUploader {

    @Autowired
    private DocumentUploadClientApi documentUploadClientApi;

    public Document upload(
        Resource resource,
        String contentType,
        String accessToken,
        String serviceAuthorizationToken,
        UserInfo userInfo) {

        try {

            MultipartFile file = new InMemoryMultipartFile(
                resource.getFilename(),
                resource.getFilename(),
                contentType,
                ByteStreams.toByteArray(resource.getInputStream())
            );

            System.out.println(format("Uploading document '%s'", file.getOriginalFilename()));
            UploadResponse uploadResponse =
                documentUploadClientApi
                    .upload(
                        accessToken,
                        serviceAuthorizationToken,
                        userInfo.getUid(),
                        Collections.singletonList(file)
                    );

            uk.gov.hmcts.reform.document.domain.Document uploadedDocument =
                uploadResponse
                    .getEmbedded()
                    .getDocuments()
                    .get(0);

            System.out.println(format("Document '%s' uploaded successfully", file.getOriginalFilename()));
            return new Document(
                uploadedDocument
                    .links
                    .self
                    .href,
                uploadedDocument
                    .links
                    .binary
                    .href,
                file.getOriginalFilename()
            );

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
