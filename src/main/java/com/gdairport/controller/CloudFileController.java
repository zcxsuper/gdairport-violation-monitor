package com.gdairport.controller;

import com.gdairport.domain.ResponseResult;
import com.gdairport.domain.dto.CreateFolderDto;
import com.gdairport.domain.dto.MoveFileDto;
import com.gdairport.domain.dto.RenameDto;
import com.gdairport.domain.entity.File;
import com.gdairport.exception.NotFoundException;
import com.gdairport.service.CloudFileService;
import com.gdairport.util.UserContextUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
@Validated
public class CloudFileController {

    private final CloudFileService cloudFileService;

    /*@GetMapping("/file/download")
    public void fileDownload(HttpServletResponse response, @RequestParam("filePath") String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("当前下载的文件不存在，请检查路径是否正确");
        }

        // 将文件写入输入流
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()))) {

            // 一次性读取到内存中
            byte[] buffer = new byte[is.available()];
            int read = is.read(buffer);

            // 清空 response
            response.reset();
            response.setCharacterEncoding("UTF-8");

            // Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
            // attachment表示以附件方式下载   inline表示在线打开   "Content-Disposition: inline; filename=文件名.mp3"
            // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));

            // 告知浏览器文件的大小
            response.addHeader("Content-Length", "" + file.length());

            OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
            response.setContentType("application/octet-stream");
            outputStream.write(buffer);
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @GetMapping("/net/download")
    public void netDownload(HttpServletResponse response, @RequestParam("fileAddress") String fileAddress, @RequestParam("filename") String filename) {

        try {
            URL url = new URL(fileAddress);
            URLConnection conn = url.openConnection();
            InputStream inputStream = conn.getInputStream();

            response.reset();
            response.setContentType(conn.getContentType());

            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));

            byte[] buffer = new byte[1024];
            int len;

            OutputStream outputStream = response.getOutputStream();

            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            inputStream.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }*/

    @GetMapping("/download")
    public void fileDownload(HttpServletResponse response, 
                             @RequestParam("id") @NotNull(message = "文件ID不能为空") Long id) throws NotFoundException {
        cloudFileService.fileDownload(response, id);
    }

    /**
     * 文件上传处理
     * @param file 上传的文件 (来自表单的 'file' 字段)
     * @param parentId 文件的父目录ID (可选, null表示根目录)
     * @return 包含新文件元数据的 ResponseEntity
     */
    @PostMapping("/upload")
    public ResponseResult<T> fileUpload(@RequestParam("file") @NotNull(message = "文件不能为空") MultipartFile file,
                                        @RequestParam(value = "parent-id", required = false) Long parentId) throws NotFoundException {
        Long userId = UserContextUtil.getUserId();
        cloudFileService.fileUpload(file, parentId, userId);
        return ResponseResult.success();
    }



    /** 创建文件夹 */
    @PostMapping("/folder")
    public ResponseResult<T> createFolder(@Valid @RequestBody CreateFolderDto folderDto) throws NotFoundException {
        Long userId = UserContextUtil.getUserId();
        cloudFileService.createFolder(folderDto, userId);
        return ResponseResult.success();
    }

    @GetMapping("/{id}")
    public ResponseResult<File> getFileById(@PathVariable @NotNull(message = "文件ID不能为空")
                                                  @Min(value = 1, message = "文件ID必须大于0") Long id) {
        File file = cloudFileService.getFileById(id);
        if (file == null) {
            throw new NotFoundException("文件或文件夹不存在, id: " + id);
        }
        return ResponseResult.success(file);
    }

    @GetMapping("/list")
    public ResponseResult<List<File>> listFiles(@RequestParam(required = false) Long id) {
        List<File> files = cloudFileService.getFileListById(id);
        return ResponseResult.success(files);
    }


    @PutMapping("/rename")
    public ResponseResult<T> renameFile(@Valid @RequestBody RenameDto renameDto) throws NotFoundException {
        Long userId = UserContextUtil.getUserId();
        cloudFileService.rename(renameDto.getId(), renameDto.getNewName(), userId);
        return ResponseResult.success();
    }

    @DeleteMapping("/{id}")
    public ResponseResult<T> deleteFile(@PathVariable @NotNull(message = "文件ID不能为空") 
                                        @Min(value = 1, message = "文件ID必须大于0") Long id) throws NotFoundException {
        Long userId = UserContextUtil.getUserId();
        cloudFileService.delete(id, userId);
        return ResponseResult.success();
    }

    @PutMapping("/move")
    public ResponseResult<T> moveFile(@Valid @RequestBody MoveFileDto moveDto) throws NotFoundException {
        Long userId = UserContextUtil.getUserId();
        cloudFileService.move(moveDto.getId(), moveDto.getNewParentId(), userId);
        return ResponseResult.success();
    }
}
