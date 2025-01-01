package file.service;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.UUID;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import file.entity.AttachmentFile;
import file.repository.AttachmentFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class S3Service {
	
	private final AmazonS3 amazonS3;
	private final AttachmentFileRepository fileRepository;
	
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    
    private final String DIR_NAME = "s3_data";
    
    // 파일 업로드
	@Transactional
	public void uploadS3File(MultipartFile file) throws Exception {
		System.out.println("S3Service : uploadS3File");
		
		if(file == null) {
			throw new Exception("파일 전달 오류 발생");
		}
		
			// file이 있다면...
		// 속성값 정보 가져오기 및 Entitiy 생성 
		/* Windows 로컬용 
		String filePath = "C://CE//97.data//" + DIR_NAME;
		*/
		/* Linux Ubuntu EC2 용 */
		String filePath = "/home/ubuntu/app/" + DIR_NAME;
		File linux_directory = new File(filePath);
		
		boolean linux_dir = linux_directory.mkdirs();
		System.out.println("Linux 디렉토리 생성 결과 : " + linux_dir);
		
		String attachmentOriginalFileName = file.getOriginalFilename();
		UUID uuid = UUID.randomUUID();
		String attachmentFileName = uuid.toString() + "_" + attachmentOriginalFileName;
		Long attachmentFileSize = file.getSize();
		
		AttachmentFile attachmentFile = AttachmentFile.builder() // Entity 생성
												.filePath(filePath)
												.attachmentOriginalFileName(attachmentOriginalFileName)
												.attachmentFileName(attachmentFileName)
												.attachmentFileSize(attachmentFileSize)
												.build();
		
		// DB 저장
//		fileRepository.save(attachmentFile);
		
		// DB에 잘 저장되었는지 확인
		Long fileNo = fileRepository.save(attachmentFile).getAttachmentFileNo();
		
		if (fileNo != null) {
			// C:/CE/97.data/s3_data에 파일 저장 : 바로 s3로 저장하는 것이 아니라 application을 통해서 저장하는 것이기 때문에, application에서 먼저 전달되어진 파일이 물리적으로 있어야 한다. 그래서 로컬에 저장해야 한다. 만약 s3로 바로 저장한다면 로컬에 저장하는 과정이 필요가 없다
			File uploadFile = new File(attachmentFile.getFilePath() + "//" + attachmentFileName);
			file.transferTo(uploadFile);
			
			// 로컬에 저장되었는지, DB에도 저장되었는지 확인
			
			// S3 전송 및 저장 (putObject) : bucketName, key(bucket 내부에 객체가 저장되는 위치(경로) + 파일명), 파일(물리적인 파일 소스) 
			String s3Key = DIR_NAME + "/" + uploadFile.getName(); // uuid 포함된 파일명
			amazonS3.putObject(bucketName, s3Key, uploadFile);
			
			// S3에 저장 후 로컬의 물리적인 파일 제거
			if (uploadFile.exists()) {
				uploadFile.delete();
			}
			
		}
		
	}
	
	// 파일 다운로드
	@Transactional
	public ResponseEntity<Resource> downloadS3File(long fileNo){
		AttachmentFile attachmentFile = null;
		Resource resource = null;
		
		
		try {
			// DB에서 파일 검색
			attachmentFile = fileRepository.findById(fileNo)
										   .orElseThrow(() -> new NoSuchElementException("파일 없음"));
			System.out.println(attachmentFile.getAttachmentFileName()); 
			// 오류! (해결 못함) 지금 findById로 DB에서 데이터를 못 가져옴. 나머지는 정상
			
			// S3의 파일 가져오기 (getObject) -> 전달
				// bucket : name
				// key : 파일 경로
			S3Object s3Object = amazonS3.getObject(bucketName, DIR_NAME + "/" + attachmentFile.getAttachmentFileName());
			
			S3ObjectInputStream s3is = s3Object.getObjectContent();
			resource = new InputStreamResource(s3is); // 리소스로 매핑
		} catch (Exception e) {
			return new ResponseEntity<Resource>(resource, null, HttpStatus.NO_CONTENT); // null 부분은 헤더
		}
		
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDisposition(ContentDisposition.builder("attachment") // attachment 문자열 필요
														.filename(attachmentFile.getAttachmentOriginalFileName())
														.build());
		
		
		return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK); // 헤더가 반드시 있어야 함
	}
	
}