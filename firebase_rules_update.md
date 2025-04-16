# Hướng dẫn cập nhật quy tắc bảo mật Firebase

Để cho phép người dùng xóa dữ liệu WaterLog, bạn cần cập nhật quy tắc bảo mật Firebase. Dưới đây là các bước thực hiện:

## Bước 1: Đăng nhập vào Firebase Console

- Truy cập [Firebase Console](https://console.firebase.google.com/)
- Chọn dự án WataCrab của bạn

## Bước 2: Điều hướng đến Firestore Database

- Trong menu bên trái, chọn "Firestore Database"
- Chuyển đến tab "Rules" (Quy tắc)

## Bước 3: Cập nhật quy tắc bảo mật

Thay thế quy tắc hiện tại bằng quy tắc sau:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Quy tắc cho collection users
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Quy tắc cho collection water_logs
    match /water_logs/{logId} {
      // Cho phép đọc nếu userId khớp với userId của người dùng đăng nhập
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      
      // Cho phép tạo mới nếu userId khớp với userId của người dùng đăng nhập
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      
      // Cho phép cập nhật nếu userId không thay đổi và khớp với userId của người dùng đăng nhập
      allow update: if request.auth != null && 
                      resource.data.userId == request.auth.uid &&
                      request.resource.data.userId == request.auth.uid;
      
      // Cho phép xóa nếu userId khớp với userId của người dùng đăng nhập
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

## Bước 4: Xuất bản quy tắc

- Nhấn nút "Publish" (Xuất bản) để áp dụng quy tắc mới

## Giải thích quy tắc:

- `allow read`: Cho phép đọc dữ liệu nếu người dùng đã đăng nhập và userId trong tài liệu khớp với ID của người dùng đăng nhập
- `allow create`: Cho phép tạo mới nếu userId trong dữ liệu mới khớp với ID của người dùng đăng nhập
- `allow update`: Cho phép cập nhật nếu userId không thay đổi và khớp với ID của người dùng đăng nhập
- `allow delete`: **CHO PHÉP XÓA** nếu userId trong tài liệu khớp với ID của người dùng đăng nhập

Sau khi áp dụng quy tắc này, người dùng sẽ có thể xóa các bản ghi WaterLog của họ. 