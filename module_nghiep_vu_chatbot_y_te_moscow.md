# Danh sách Module/Nghiệp vụ theo MoSCoW  
## Dự án: Chatbot hỗ trợ tra cứu thông tin y tế từ FHIR Server hoặc Database mô phỏng

---

## I. MUST HAVE — Bắt buộc phải có

### M1: Module User & Authentication - Người dùng và đăng nhập

M1.1: Cho phép người dùng đăng nhập vào hệ thống bằng tài khoản được cấp.  

M1.2: Cho phép người dùng đăng xuất khỏi hệ thống.  

M1.3: Quản lý thông tin cơ bản của người dùng như họ tên, email, vai trò, nhóm người dùng.  

M1.4: Phân quyền người dùng theo vai trò như User, Doctor, Admin.  

M1.5: Kiểm tra quyền truy cập trước khi người dùng sử dụng các chức năng tra cứu dữ liệu y tế.  

M1.6: Cho phép Admin khóa hoặc mở khóa tài khoản người dùng khi cần.  

---

### M2: Module Conversation - Quản lý hội thoại chatbot

M2.1: Tạo phiên hội thoại mới khi người dùng bắt đầu chat.  

M2.2: Tiếp nhận câu hỏi của người dùng bằng ngôn ngữ tự nhiên.  

M2.3: Lưu câu hỏi của người dùng vào lịch sử hội thoại.  

M2.4: Gửi câu hỏi đến AI service để xử lý.  

M2.5: Nhận câu trả lời từ AI service và hiển thị cho người dùng.  

M2.6: Lưu câu trả lời của chatbot vào lịch sử hội thoại.  

M2.7: Cho phép người dùng tiếp tục hội thoại cũ.  

M2.8: Cho phép người dùng xem lại danh sách các cuộc hội thoại trước đó.  

M2.9: Lưu thời gian tạo, thời gian cập nhật và trạng thái của từng phiên chat.  

---

### M3: Module Message History - Lưu trữ tin nhắn và lịch sử trao đổi

M3.1: Lưu nội dung từng tin nhắn của người dùng.  

M3.2: Lưu nội dung từng phản hồi của chatbot.  

M3.3: Phân biệt loại tin nhắn: user message, assistant message, system message.  

M3.4: Lưu thời gian gửi của từng tin nhắn.  

M3.5: Lưu thông tin phiên hội thoại mà tin nhắn thuộc về.  

M3.6: Cho phép truy xuất lại toàn bộ nội dung hội thoại theo conversation ID.  

M3.7: Hỗ trợ hiển thị lịch sử tin nhắn theo đúng thứ tự thời gian.  

---

### M4: Module Medical Data Query - Truy vấn dữ liệu y tế

M4.1: Nhận yêu cầu tra cứu dữ liệu y tế từ phân hệ hội thoại.  

M4.2: Xác định loại dữ liệu cần tra cứu như bệnh nhân, lịch sử khám, thuốc, xét nghiệm, chẩn đoán.  

M4.3: Tra cứu thông tin cơ bản của bệnh nhân.  

M4.4: Tra cứu lịch sử khám bệnh của bệnh nhân.  

M4.5: Tra cứu danh sách thuốc hoặc đơn thuốc của bệnh nhân.  

M4.6: Tra cứu kết quả xét nghiệm hoặc chỉ số y tế.  

M4.7: Tra cứu thông tin chẩn đoán, bệnh lý hoặc tình trạng sức khỏe.  

M4.8: Kiểm tra quyền truy cập trước khi trả dữ liệu y tế cho người dùng.  

M4.9: Chuẩn hóa dữ liệu y tế trước khi đưa vào prompt cho AI.  

---

### M5: Module FHIR/Medical Database Integration - Kết nối FHIR Server hoặc database mô phỏng

M5.1: Kết nối đến FHIR Server để lấy dữ liệu y tế.  

M5.2: Kết nối đến database trung gian trong trường hợp dùng dữ liệu mô phỏng.  

M5.3: Truy vấn tài nguyên Patient để lấy thông tin bệnh nhân.  

M5.4: Truy vấn tài nguyên Encounter để lấy lịch sử khám.  

M5.5: Truy vấn tài nguyên Medication hoặc MedicationRequest để lấy thông tin thuốc.  

M5.6: Truy vấn tài nguyên Observation để lấy kết quả xét nghiệm hoặc chỉ số sinh tồn.  

M5.7: Truy vấn tài nguyên Condition để lấy thông tin chẩn đoán.  

M5.8: Xử lý lỗi khi FHIR Server hoặc database không phản hồi.  

M5.9: Trả dữ liệu về cho hệ thống theo định dạng thống nhất.  

---

### M6: Module AI Integration - Tích hợp AI/LLM Service

M6.1: Nhận câu hỏi, ngữ cảnh hội thoại và dữ liệu y tế liên quan.  

M6.2: Xây dựng prompt để gửi đến AI service.  

M6.3: Gọi API đến AI service nội bộ hoặc mô hình LLM bên ngoài.  

M6.4: Nhận phản hồi từ AI service.  

M6.5: Xử lý phản hồi AI trước khi trả về cho người dùng.  

M6.6: Lưu thông tin model được sử dụng cho mỗi lần gọi.  

M6.7: Xử lý lỗi khi AI service không phản hồi.  

M6.8: Giới hạn dữ liệu gửi vào AI để tránh vượt quá token limit.  

---

### M7: Module Usage Tracking - Theo dõi mức sử dụng AI

M7.1: Ghi nhận mỗi lần người dùng gửi request đến AI.  

M7.2: Lưu số token đầu vào của mỗi request.  

M7.3: Lưu số token đầu ra của mỗi response.  

M7.4: Tính tổng token tiêu thụ cho từng lần gọi AI.  

M7.5: Lưu tên model được sử dụng trong mỗi request.  

M7.6: Theo dõi số lượt gọi AI theo từng người dùng.  

M7.7: Theo dõi số lượt gọi AI theo từng nhóm người dùng.  

M7.8: Thống kê usage theo ngày, tháng hoặc khoảng thời gian.  

---

### M8: Module Cost Management - Quản lý chi phí AI

M8.1: Cấu hình đơn giá token theo từng model.  

M8.2: Tính chi phí cho mỗi lần gọi AI dựa trên input token và output token.  

M8.3: Lưu chi phí phát sinh của từng request.  

M8.4: Theo dõi tổng chi phí theo từng người dùng.  

M8.5: Theo dõi tổng chi phí theo từng model.  

M8.6: Theo dõi tổng chi phí theo ngày, tháng hoặc khoảng thời gian.  

M8.7: Hiển thị thống kê chi phí cho Admin.  

---

### M9: Module Quota Management - Quản lý hạn mức sử dụng

M9.1: Thiết lập quota sử dụng AI cho từng người dùng.  

M9.2: Thiết lập quota sử dụng AI cho từng nhóm người dùng.  

M9.3: Kiểm tra quota trước khi cho phép gọi AI.  

M9.4: Trừ quota sau mỗi lần người dùng gọi AI thành công.  

M9.5: Chặn request nếu người dùng vượt quá quota.  

M9.6: Lưu lịch sử thay đổi quota.  

M9.7: Cho phép Admin điều chỉnh quota khi cần.  

---

### M10: Module Rate Limiting - Giới hạn tần suất request

M10.1: Giới hạn số request của người dùng trong một khoảng thời gian.  

M10.2: Giới hạn số request theo IP hoặc tài khoản.  

M10.3: Từ chối request khi người dùng gửi quá nhiều yêu cầu liên tục.  

M10.4: Trả thông báo phù hợp khi người dùng bị giới hạn tần suất.  

M10.5: Ghi log các trường hợp bị chặn do vượt rate limit.  

---

### M11: Module Logging & Error Handling - Ghi log và xử lý lỗi

M11.1: Ghi log khi người dùng đăng nhập, đăng xuất.  

M11.2: Ghi log khi người dùng gửi câu hỏi.  

M11.3: Ghi log mỗi lần hệ thống gọi AI service.  

M11.4: Ghi log mỗi lần hệ thống truy vấn dữ liệu y tế.  

M11.5: Ghi log lỗi khi AI service không phản hồi.  

M11.6: Ghi log lỗi khi FHIR Server hoặc database gặp sự cố.  

M11.7: Hiển thị thông báo lỗi thân thiện cho người dùng.  

M11.8: Đảm bảo lỗi hệ thống không làm lộ dữ liệu nhạy cảm.  

---

### M12: Module Database Design - Thiết kế cơ sở dữ liệu

M12.1: Thiết kế bảng user để lưu thông tin người dùng.  

M12.2: Thiết kế bảng role hoặc permission để quản lý phân quyền.  

M12.3: Thiết kế bảng conversation để lưu phiên hội thoại.  

M12.4: Thiết kế bảng message để lưu nội dung chat.  

M12.5: Thiết kế bảng usage_log để lưu lượt gọi AI, token và model.  

M12.6: Thiết kế bảng quota để lưu hạn mức sử dụng.  

M12.7: Thiết kế bảng cost_log để lưu chi phí phát sinh.  

M12.8: Thiết kế bảng cache để lưu kết quả truy vấn hoặc câu trả lời thường gặp.  

M12.9: Thiết kế bảng audit_log để lưu nhật ký thao tác quan trọng.  

---

## II. SHOULD HAVE — Nên có

### M13: Module Admin Dashboard - Trang quản trị và giám sát

M13.1: Hiển thị tổng số người dùng trong hệ thống.  

M13.2: Hiển thị tổng số cuộc hội thoại đã tạo.  

M13.3: Hiển thị tổng số lượt gọi AI.  

M13.4: Hiển thị tổng token đã tiêu thụ.  

M13.5: Hiển thị tổng chi phí AI đã sử dụng.  

M13.6: Hiển thị danh sách người dùng sử dụng nhiều tài nguyên nhất.  

M13.7: Hiển thị thống kê usage theo ngày hoặc tháng.  

M13.8: Hiển thị thống kê lỗi hệ thống.  

M13.9: Cho phép Admin lọc thống kê theo user, model hoặc thời gian.  

---

### M14: Module Cache Management - Quản lý cache

M14.1: Kiểm tra cache trước khi gọi AI hoặc truy vấn dữ liệu y tế.  

M14.2: Lưu kết quả câu hỏi thường gặp vào cache.  

M14.3: Lưu kết quả truy vấn dữ liệu y tế ít thay đổi vào cache.  

M14.4: Thiết lập thời gian sống cho cache.  

M14.5: Tự động xóa cache khi hết hạn.  

M14.6: Cho phép Admin cấu hình cache TTL.  

M14.7: Giúp giảm số lần gọi AI và giảm chi phí vận hành.  

---

### M15: Module Context Management - Quản lý ngữ cảnh hội thoại

M15.1: Lấy các tin nhắn gần nhất trong hội thoại để làm context.  

M15.2: Giới hạn số lượng tin nhắn gửi vào AI.  

M15.3: Loại bỏ các tin nhắn không cần thiết trước khi gọi AI.  

M15.4: Tóm tắt hội thoại dài thành summary ngắn.  

M15.5: Lưu summary hội thoại vào database.  

M15.6: Sử dụng summary thay cho toàn bộ lịch sử khi hội thoại quá dài.  

M15.7: Giúp giảm token và tối ưu chi phí gọi AI.  

---

### M16: Module Model Routing - Điều phối model AI

M16.1: Phân loại request theo loại câu hỏi của người dùng.  

M16.2: Chọn model phù hợp với từng loại request.  

M16.3: Dùng model nhẹ cho câu hỏi đơn giản.  

M16.4: Dùng model mạnh hơn cho câu hỏi cần tổng hợp dữ liệu phức tạp.  

M16.5: Cho phép Admin cấu hình rule chọn model.  

M16.6: Lưu lại model đã được chọn cho mỗi request.  

M16.7: Giúp tối ưu chi phí và hiệu năng hệ thống.  

---

### M17: Module Retry & Fallback - Gọi lại và xử lý dự phòng

M17.1: Tự động retry khi AI service lỗi tạm thời.  

M17.2: Tự động retry khi FHIR Server hoặc database lỗi kết nối tạm thời.  

M17.3: Giới hạn số lần retry để tránh làm quá tải hệ thống.  

M17.4: Chuyển sang model dự phòng khi model chính không phản hồi.  

M17.5: Trả thông báo phù hợp cho người dùng khi không thể xử lý yêu cầu.  

M17.6: Ghi log đầy đủ các lần retry và fallback.  

---

### M18: Module Audit Log - Nhật ký kiểm toán

M18.1: Ghi lại thao tác xem dữ liệu bệnh nhân.  

M18.2: Ghi lại thao tác thay đổi quota người dùng.  

M18.3: Ghi lại thao tác thay đổi cấu hình model.  

M18.4: Ghi lại thao tác thay đổi cấu hình hệ thống.  

M18.5: Ghi lại người thực hiện, thời gian thực hiện và nội dung thao tác.  

M18.6: Cho phép Admin tra cứu audit log theo người dùng, thời gian hoặc loại hành động.  

M18.7: Hỗ trợ truy vết khi có sự cố hoặc nghi ngờ truy cập sai quyền.  

---

### M19: Module Alert Management - Cảnh báo hệ thống

M19.1: Cảnh báo khi người dùng gần vượt quota.  

M19.2: Cảnh báo khi người dùng đã vượt quota.  

M19.3: Cảnh báo khi chi phí AI vượt ngưỡng cấu hình.  

M19.4: Cảnh báo khi AI service gặp lỗi liên tục.  

M19.5: Cảnh báo khi FHIR Server hoặc database không phản hồi.  

M19.6: Hiển thị cảnh báo trên dashboard Admin.  

M19.7: Lưu lịch sử các cảnh báo đã phát sinh.  

---

### M20: Module System Configuration - Cấu hình hệ thống

M20.1: Cấu hình AI provider đang sử dụng.  

M20.2: Cấu hình model mặc định.  

M20.3: Cấu hình model dự phòng.  

M20.4: Cấu hình giới hạn token đầu vào.  

M20.5: Cấu hình giới hạn token đầu ra.  

M20.6: Cấu hình quota mặc định cho người dùng mới.  

M20.7: Cấu hình cache TTL.  

M20.8: Cấu hình timeout khi gọi API.  

M20.9: Cấu hình số lần retry tối đa.  

M20.10: Cấu hình địa chỉ FHIR Server hoặc database mô phỏng.  

---

## III. COULD HAVE — Có thì tốt

### M21: Module Conversation Search - Tìm kiếm hội thoại

M21.1: Cho phép người dùng tìm kiếm hội thoại cũ theo từ khóa.  

M21.2: Cho phép lọc hội thoại theo thời gian.  

M21.3: Cho phép lọc hội thoại theo chủ đề hoặc bệnh nhân liên quan.  

M21.4: Hiển thị danh sách kết quả tìm kiếm hội thoại.  

M21.5: Cho phép mở lại hội thoại từ kết quả tìm kiếm.  

---

### M22: Module Feedback - Đánh giá câu trả lời chatbot

M22.1: Cho phép người dùng đánh giá câu trả lời là hữu ích hoặc không hữu ích.  

M22.2: Cho phép người dùng gửi góp ý cho câu trả lời.  

M22.3: Lưu feedback gắn với từng message.  

M22.4: Cho phép Admin xem thống kê feedback.  

M22.5: Hỗ trợ đánh giá chất lượng phản hồi của chatbot.  

---

### M23: Module Export Conversation - Xuất lịch sử hội thoại

M23.1: Cho phép người dùng xuất hội thoại ra file PDF.  

M23.2: Cho phép người dùng xuất hội thoại ra file Excel hoặc CSV.  

M23.3: Cho phép chọn khoảng thời gian hoặc phiên chat cần xuất.  

M23.4: Ghi log thao tác xuất dữ liệu.  

M23.5: Kiểm tra quyền trước khi cho phép xuất hội thoại có dữ liệu y tế.  

---

### M24: Module Advanced Analytics - Thống kê nâng cao

M24.1: Thống kê loại câu hỏi được người dùng hỏi nhiều nhất.  

M24.2: Thống kê tỷ lệ lỗi theo service.  

M24.3: Thống kê model nào được sử dụng nhiều nhất.  

M24.4: Thống kê chi phí trung bình trên mỗi cuộc hội thoại.  

M24.5: Thống kê thời gian phản hồi trung bình.  

M24.6: Hiển thị biểu đồ usage, token, cost, error rate trên dashboard.  

---

### M25: Module Notification - Thông báo

M25.1: Gửi thông báo khi người dùng gần vượt quota.  

M25.2: Gửi thông báo khi hệ thống phát sinh lỗi nghiêm trọng.  

M25.3: Gửi thông báo cho Admin khi chi phí vượt ngưỡng.  

M25.4: Hỗ trợ thông báo qua email.  

M25.5: Hỗ trợ thông báo trực tiếp trên giao diện hệ thống.  

---

### M26: Module Backup & Restore - Sao lưu và khôi phục dữ liệu

M26.1: Sao lưu database theo lịch định kỳ.  

M26.2: Cho phép Admin tải file backup.  

M26.3: Cho phép khôi phục dữ liệu từ bản backup.  

M26.4: Ghi log quá trình backup và restore.  

M26.5: Cảnh báo khi backup thất bại.  

---

## IV. WON’T HAVE / LATER — Chưa triển khai trong phạm vi hiện tại

### M27: Module Auto Diagnosis - Chẩn đoán bệnh tự động

M27.1: Hệ thống không tự động kết luận bệnh thay bác sĩ.  

M27.2: Hệ thống không đưa ra chẩn đoán y khoa chính thức.  

M27.3: Chức năng này để phát triển sau nếu có kiểm định chuyên môn.  

---

### M28: Module Auto Prescription - Kê đơn thuốc tự động

M28.1: Hệ thống không tự động kê đơn thuốc cho bệnh nhân.  

M28.2: Hệ thống không tự động thay đổi thuốc đang dùng.  

M28.3: Hệ thống chỉ hỗ trợ tra cứu thông tin thuốc đã có trong dữ liệu.  

---

### M29: Module Real Hospital Integration - Tích hợp bệnh viện thật

M29.1: Chưa kết nối trực tiếp với hệ thống bệnh viện thật.  

M29.2: Chưa đồng bộ dữ liệu bệnh án thật quy mô lớn.  

M29.3: Bản demo ưu tiên dùng FHIR Server test hoặc database mô phỏng.  

---

### M30: Module Mobile Application - Ứng dụng di động

M30.1: Chưa xây dựng app mobile riêng cho Android hoặc iOS.  

M30.2: Bản đầu tập trung vào giao diện web.  

M30.3: Mobile app có thể phát triển ở giai đoạn sau.  

---

### M31: Module Voice Chatbot - Chatbot giọng nói

M31.1: Chưa hỗ trợ nhập câu hỏi bằng giọng nói.  

M31.2: Chưa hỗ trợ đọc câu trả lời bằng giọng nói.  

M31.3: Chức năng speech-to-text và text-to-speech để phát triển sau.  

---

### M32: Module AI Training/Fine-tuning - Huấn luyện hoặc tinh chỉnh mô hình

M32.1: Không huấn luyện mô hình AI từ đầu.  

M32.2: Không fine-tune mô hình y tế chuyên sâu trong phạm vi bản đầu.  

M32.3: Hệ thống sử dụng AI service nội bộ hoặc LLM API đã có sẵn.  

---

## V. Bản rút gọn các module chính

### M1: Module User & Authentication - Người dùng và đăng nhập

M1.1: Đăng nhập, đăng xuất hệ thống.  

M1.2: Quản lý thông tin người dùng.  

M1.3: Phân quyền theo vai trò User, Doctor, Admin.  

M1.4: Kiểm tra quyền truy cập chức năng.  

M1.5: Khóa hoặc mở khóa tài khoản người dùng.  

---

### M2: Module Conversation - Hội thoại chatbot

M2.1: Tạo phiên hội thoại mới.  

M2.2: Tiếp nhận câu hỏi bằng ngôn ngữ tự nhiên.  

M2.3: Gửi câu hỏi đến AI service.  

M2.4: Hiển thị câu trả lời từ chatbot.  

M2.5: Lưu và xem lại lịch sử hội thoại.  

---

### M3: Module Medical Data Query - Truy vấn dữ liệu y tế

M3.1: Tra cứu thông tin bệnh nhân.  

M3.2: Tra cứu lịch sử khám.  

M3.3: Tra cứu thuốc và đơn thuốc.  

M3.4: Tra cứu xét nghiệm.  

M3.5: Tra cứu chẩn đoán.  

M3.6: Chuẩn hóa dữ liệu y tế trước khi gửi vào AI.  

---

### M4: Module AI Integration - Tích hợp AI

M4.1: Xây dựng prompt cho AI.  

M4.2: Gọi AI service nội bộ hoặc LLM API bên ngoài.  

M4.3: Nhận và xử lý phản hồi AI.  

M4.4: Giới hạn dữ liệu gửi vào model.  

M4.5: Xử lý lỗi khi AI service không phản hồi.  

---

### M5: Module Usage & Quota Management - Theo dõi usage, token, chi phí và quota

M5.1: Ghi nhận số lượt gọi AI.  

M5.2: Theo dõi input token và output token.  

M5.3: Tính chi phí theo user, model và thời gian.  

M5.4: Thiết lập quota cho người dùng hoặc nhóm người dùng.  

M5.5: Chặn request khi vượt quota.  

---

### M6: Module Operation Optimization - Tối ưu vận hành

M6.1: Cache kết quả truy vấn thường gặp.  

M6.2: Rút gọn context trước khi gửi vào AI.  

M6.3: Lưu summary hội thoại dài.  

M6.4: Routing request theo loại truy vấn.  

M6.5: Retry và fallback khi dịch vụ ngoài gặp lỗi.  

---

### M7: Module Admin Dashboard - Quản trị và giám sát

M7.1: Hiển thị thống kê số lượt chat.  

M7.2: Hiển thị token và chi phí tiêu thụ.  

M7.3: Hiển thị tần suất truy vấn.  

M7.4: Hiển thị cảnh báo vượt quota.  

M7.5: Quản lý cấu hình model, quota, cache và cảnh báo.  

---

### M8: Module Logging & Audit - Nhật ký hệ thống và kiểm toán

M8.1: Ghi log đăng nhập, gửi câu hỏi, gọi AI.  

M8.2: Ghi log truy vấn dữ liệu y tế.  

M8.3: Ghi log lỗi hệ thống.  

M8.4: Ghi audit log thao tác Admin.  

M8.5: Hỗ trợ truy vết khi có lỗi hoặc truy cập bất thường.  

---

### M9: Module Security & Access Control - Bảo mật và kiểm soát truy cập

M9.1: Xác thực người dùng.  

M9.2: Phân quyền theo vai trò.  

M9.3: Kiểm tra quyền truy cập dữ liệu bệnh nhân.  

M9.4: Che giấu dữ liệu nhạy cảm khi cần.  

M9.5: Rate limiting để chống spam request.  

---

### M10: Module Deployment - Đóng gói và triển khai

M10.1: Đóng gói backend bằng Docker.  

M10.2: Đóng gói frontend bằng Docker.  

M10.3: Đóng gói database bằng Docker.  

M10.4: Cấu hình Docker Compose.  

M10.5: Migration và seed dữ liệu mô phỏng.  
