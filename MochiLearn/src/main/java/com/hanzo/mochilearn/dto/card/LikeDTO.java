package com.hanzo.mochilearn.dto.card;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeDTO {
	 	private Integer likeId;
	    private Integer memberId;
	    private Integer cardId;
	    private LocalDateTime likeTime;
}
