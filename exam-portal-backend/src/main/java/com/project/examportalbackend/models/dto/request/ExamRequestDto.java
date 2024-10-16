package com.project.examportalbackend.models.dto.request;

import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ExamRequestDto {

    private long examId = -1;
    @NotNull(message = "Exam must have a title")
    @NotEmpty(message = "Exam title can't be empty")
    @Size(max = 25, message = "Exam title can't have more than 25 characters")
    private String title;

    @NotNull(message = "Exam must have a description")
    @NotEmpty(message = "Exam description can't be empty")
    @Size(max = 250, message = "Exam title can't have more than 250 characters")
    private String description;

    @NotNull(message = "Exam must have a registration deadline date")
    private Date registrationDeadlineDate;

    @NotNull(message = "Exam must have a start date")
    private Date startDate;

}
