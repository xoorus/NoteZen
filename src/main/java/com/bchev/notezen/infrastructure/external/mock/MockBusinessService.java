package com.bchev.notezen.infrastructure.external.mock;

import com.bchev.notezen.application.controller.dto.ReviewDTO;
import com.bchev.notezen.application.controller.dto.ReviewerDTO;
import com.bchev.notezen.application.controller.dto.StarRatingDTO;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.BusinessProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

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
        return List.of(
                Map.of("name", accountId + "/locations/loc-paris", "title", "Le Petit Bistro - Paris"),
                Map.of("name", accountId + "/locations/loc-bordeaux", "title", "Le Petit Bistro - Bordeaux"),
                Map.of("name", accountId + "/locations/loc-lyon", "title", "Le Petit Bistro - Lyon")
        );
    }

    @Override
    public List<Review> fetchReviews(String accountId, String locationId, String accessToken) {
        List<ReviewDTO> reviewsDTO = new ArrayList<>();
        if (locationId.contains("loc-paris")) {
            reviewsDTO.add(createReview("1", "Alice L.", "Excellent service, je recommande !", "FIVE"));
            reviewsDTO.add(createReview("2", "Marc A.", "Un peu bruyant mais très bon.", "FOUR"));
        }
        reviewsDTO.add(createReview("21", "Thomas P.", "Déçu par l'accueil.", "TWO"));
        reviewsDTO.add(createReview("22", "Jean Dupont", "Excellent accueil, je reviendrai !", "FIVE"));
        reviewsDTO.add(createReview("23", "Alice Martin", "Trop d'attente pour être servi...", "TWO"));
        reviewsDTO.add(createReview("24", "Julie Martin", "Super service 😊 je recommande !!", "FIVE"));
        reviewsDTO.add(createReview("25", "Kevin L.", "Good experience overall. Staff was friendly and the place was clean 👍", "FOUR"));
        reviewsDTO.add(createReview("3", "Marc Dubois", "Franchement bof... j'attendais mieux pour le prix.", "TWO"));
        reviewsDTO.add(createReview("4", "Samantha K.", "Amazing place!! Will definitely come back next time I'm in town 🔥", "FIVE"));
        reviewsDTO.add(createReview("5", "Lucie Bernard", "Très bon accueil, par contre un peu d'attente. Mais bon ça valait le coup quand même 🙂", "FOUR"));
        reviewsDTO.add(createReview("6", "Tom R.", "Not bad. Nothing special but ok.", "THREE"));
        reviewsDTO.add(createReview("7", "Alexandre Petit", "Service nickel 👌 équipe sympa et pro.", "FIVE"));
        reviewsDTO.add(createReview("8", "Sarah M.", "Honestly I expected more. The reviews were very good but my experience was just average.", "THREE"));
        reviewsDTO.add(createReview("9", "Jean-Claude", "Je suis venu avec ma famille dimanche. L'accueil était sympa mais on a attendu presque 30 minutes avant d'être servis... un peu long quand même. Après ça les produits étaient très bons donc je suis partagé 🤔", "THREE"));
        reviewsDTO.add(createReview("10", "Emily Watson", "Loved it! Great atmosphere, nice people and the service was super quick. Will come again for sure ❤️", "FIVE"));
        reviewsDTO.add(createReview("11", "Patrick Leroy", "Pas mal mais y avait une erreur dans ma commande 😅", "THREE"));
        reviewsDTO.add(createReview("12", "Lucas B.", "Franchement top ! Rien à dire. Qualité au rendez vous, personnel agréable, prix corrects 👍", "FIVE"));
        reviewsDTO.add(createReview("13", "Nathan", "meh.", "TWO"));
        reviewsDTO.add(createReview("14", "Chloé R.", "Très bonne surprise !! je connaissais pas du tout et je reviendrai avec plaisir 😊", "FIVE"));
        reviewsDTO.add(createReview("15", "Oliver Scott", "Service was slow and nobody really seemed to care. Maybe it was just a bad day but still disappointing.", "TWO"));
        reviewsDTO.add(createReview("16", "Mélanie", "Bon endroit mais un peu bruillant 😅", "FOUR"));
        reviewsDTO.add(createReview("17", "David P.", "Really good experience. I went there with colleagues after work and everyone liked it. Food was great, service friendly, prices reasonable. The only small downside was the waiting time but honestly it wasn't a big deal.", "FOUR"));
        reviewsDTO.add(createReview("18", "Sophie Laurent", "Je mets 5⭐ parce que vraiment tout était parfait. L'accueil, l'ambiance, la qualité du service... ça fait plaisir de voir des endroits où les gens prennent encore le temps de bien faire les choses. Je recommande sans hésiter et je reviendrai avec des amis la prochaine fois.", "FIVE"));
        reviewsDTO.add(createReview("19", "Mike", "ok 👍", "THREE"));
        reviewsDTO.add(createReview("20", "Aurélien G.", "Très déçu 😕 on m'avait conseillé cet endroit mais je pense que je suis tombé un mauvais jour. Personnel un peu débordé et service lent.", "TWO"));

        return reviewsDTO.stream().map(ReviewDTO::toReview).toList();
    }

    @Override
    public void postReply(String accountId, String locationId, String reviewId, String text, String accessToken) {
        log.info("MOCK API [Google] : Réponse envoyée pour l'avis {} (Etablissement: {}). Message : {}",
                reviewId, locationId, text);
    }

    private ReviewDTO createReview(String id, String author, String comment, String rating) {
        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(id);
        dto.setReviewer(new ReviewerDTO(author, "", false));
        dto.setComment(comment);
        dto.setStarRating(StarRatingDTO.valueOf(rating));
        dto.setCreateTime(java.time.Instant.now().toString());
        return dto;
    }
}