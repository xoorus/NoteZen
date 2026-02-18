package com.bchev.notezen.web.google.api;


import com.bchev.notezen.app.ReviewManager;
import com.bchev.notezen.web.google.DTO.ReviewDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v4/accounts/{accountId}/locations/{locationId}/reviews")
public class GoogleReviewApiController {

    ReviewManager reviewManager;

    public GoogleReviewApiController(ReviewManager reviewManager){
        this.reviewManager = reviewManager;
    }

    @GetMapping
    public List<ReviewDTO> GetReviews(
            @PathVariable String accountId,
            @PathVariable String locationId,
            @RequestParam(required = false) String pageToken) {

        return null;
    }

}
