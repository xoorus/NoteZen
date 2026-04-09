package com.bchev.notezen.infrastructure.external.mock;

import com.bchev.notezen.application.controller.dto.ReviewDTO;
import com.bchev.notezen.application.controller.dto.ReviewReplyDTO;
import com.bchev.notezen.application.controller.dto.ReviewerDTO;
import com.bchev.notezen.application.controller.dto.StarRatingDTO;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.ReviewPage;
import com.bchev.notezen.domain.service.BusinessProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile("local")
@Slf4j
public class MockBusinessService implements BusinessProvider {

    @Override
    public String fetchAccountId(String accessToken) {
        return "accounts/mock-user-123";
    }

    @Override
    public List<Map<String, Object>> fetchLocations(String accountId, String accessToken) {
        log.info("fetchLocations");
        return List.of(
                Map.of("name", accountId + "/locations/loc-paris", "title", "Le Petit Bistro - Paris"),
                Map.of("name", accountId + "/locations/loc-bordeaux", "title", "Le Petit Bistro - Bordeaux"),
                Map.of("name", accountId + "/locations/loc-lyon", "title", "Le Petit Bistro - Lyon")
        );
    }

    @Override
    public ReviewPage fetchReviews(String accountId, String locationId, String accessToken, String pageToken) {
        log.info("fetchReviews");
        Instant oneMonthAgo = Instant.now()
                .atZone(ZoneId.systemDefault())
                .minusMonths(1)
                .toInstant();
        Instant now = java.time.Instant.now();
        
        List<ReviewDTO> reviewsDTO = new ArrayList<>();
        reviewsDTO.add(createReviewWithReply("1", "Alice L.", "REPLY Excellent service, je recommande !", "FIVE", now,
                "Merci beaucoup Alice ! Au plaisir de vous revoir."));

        reviewsDTO.add(createReviewWithReply("2", "Marc A.", "REPLY Un peu bruyant mais très bon.", "FOUR", now,
                "Merci pour votre retour Marc, nous travaillons sur l'acoustique !"));

        reviewsDTO.add(createReviewWithReply("21", "Thomas P.", "REPLY ONE MONTH AGO Déçu par l'accueil.", "TWO", oneMonthAgo,
                "Nous sommes navrés, Thomas. Nous avons renforcé l'équipe depuis."));

        reviewsDTO.add(createReview("21", "Thomas P.", "ONE MONTH AGO Déçu par l'accueil.", "TWO", oneMonthAgo));
        reviewsDTO.add(createReview("22", "Jean Dupont", "ONE MONTH AGO Excellent accueil, je reviendrai !", "FIVE", oneMonthAgo));
        reviewsDTO.add(createReview("23", "Alice Martin", "ONE MONTH AGO Trop d'attente pour être servi...", "TWO", oneMonthAgo));
        reviewsDTO.add(createReview("24", "Julie Martin", "ONE MONTH AGO Super service 😊 je recommande !!", "FIVE", oneMonthAgo));
        reviewsDTO.add(createReview("25", "Kevin L.", "ONE MONTH AGO Good experience overall. Staff was friendly and the place was clean 👍", "FOUR", oneMonthAgo));
        reviewsDTO.add(createReview("4", "Samantha K.", "ONE MONTH AGO Amazing place!! Will definitely come back next time I'm in town 🔥", "FIVE", oneMonthAgo));
        reviewsDTO.add(createReview("5", "Lucie Bernard", "ONE MONTH AGO Très bon accueil, par contre un peu d'attente. Mais bon ça valait le coup quand même 🙂", "FOUR", oneMonthAgo));
        reviewsDTO.add(createReview("6", "Tom R.", "ONE MONTH AGO Not bad. Nothing special but ok.", "THREE", oneMonthAgo));
        reviewsDTO.add(createReview("7", "Alexandre Petit", "ONE MONTH AGO Service nickel 👌 équipe sympa et pro.", "FIVE", oneMonthAgo));
        reviewsDTO.add(createReview("8", "Sarah M.", "Honestly I expected more. The reviews were very good but my experience was just average.", "THREE", now));
        reviewsDTO.add(createReview("9", "Jean-Claude", "Je suis venu avec ma famille dimanche. L'accueil était sympa mais on a attendu presque 30 minutes avant d'être servis... un peu long quand même. Après ça les produits étaient très bons donc je suis partagé 🤔", "THREE", now));
        reviewsDTO.add(createReview("10", "Emily Watson", "Loved it! Great atmosphere, nice people and the service was super quick. Will come again for sure ❤️", "FIVE", now));
        reviewsDTO.add(createReview("11", "Patrick Leroy", "Pas mal mais y avait une erreur dans ma commande 😅", "THREE", now));
        reviewsDTO.add(createReview("12", "Lucas B.", "Franchement top ! Rien à dire. Qualité au rendez vous, personnel agréable, prix corrects 👍", "FIVE", now));
        reviewsDTO.add(createReview("13", "Nathan", "meh.", "TWO", now));
        reviewsDTO.add(createReview("14", "Chloé R.", "Très bonne surprise !! je connaissais pas du tout et je reviendrai avec plaisir 😊", "FIVE", now));
        reviewsDTO.add(createReview("15", "Oliver Scott", "Service was slow and nobody really seemed to care. Maybe it was just a bad day but still disappointing.", "TWO", now));
        reviewsDTO.add(createReview("16", "Mélanie", "Bon endroit mais un peu bruillant 😅", "FOUR", now));
        reviewsDTO.add(createReview("17", "David P.", "Really good experience. I went there with colleagues after work and everyone liked it. Food was great, service friendly, prices reasonable. The only small downside was the waiting time but honestly it wasn't a big deal.", "FOUR", now));
        reviewsDTO.add(createReview("18", "Sophie Laurent", "Je mets 5⭐ parce que vraiment tout était parfait. L'accueil, l'ambiance, la qualité du service... ça fait plaisir de voir des endroits où les gens prennent encore le temps de bien faire les choses. Je recommande sans hésiter et je reviendrai avec des amis la prochaine fois.", "FIVE", now));
        reviewsDTO.add(createReview("19", "Mike", "ok 👍", "THREE", now));
        reviewsDTO.add(createReview("20", "Aurélien G.", "Très déçu 😕 on m'avait conseillé cet endroit mais je pense que je suis tombé un mauvais jour. Personnel un peu débordé et service lent.", "TWO", now));

        log.info(reviewsDTO.size() + " avis trouvés");
        return new ReviewPage(reviewsDTO.stream().map(ReviewDTO::toReview).toList(), null);
    }

    @Override
    public void postReply(String accountId, String locationId, String reviewId, String text, String accessToken) {
        log.info("postReply");
        log.info("MOCK API [Google] : Réponse envoyée pour l'avis {} (Etablissement: {}). Message : {}",
                reviewId, locationId, text);
    }

    private ReviewDTO createReview(String id, String author, String comment, String rating, Instant createTime) {
        log.info("createReview");
        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(id);
        dto.setReviewer(new ReviewerDTO(author, "", false));
        dto.setComment(comment);
        dto.setStarRating(StarRatingDTO.valueOf(rating));
        dto.setCreateTime(createTime.toString());
        return dto;
    }

    // Méthode simulant un avis avec une réponse existante (Google My Business structure)
    private ReviewDTO createReviewWithReply(String id, String author, String comment, String rating, Instant createTime, String replyText) {
        ReviewDTO dto = createReview(id, author, comment, rating, createTime);

        // Simulation du champ 'reviewReply' de l'API Google
        ReviewReplyDTO reply = new ReviewReplyDTO();
        reply.setComment(replyText);
        reply.setUpdateTime(createTime.plusSeconds(3600).toString()); // Réponse 1h après
        dto.setReviewReply(reply);

        return dto;
    }
}