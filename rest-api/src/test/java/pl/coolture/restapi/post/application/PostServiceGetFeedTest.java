package pl.coolture.restapi.post.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.coolture.restapi.common.exceptions.BadRequestException;
import pl.coolture.restapi.common.exceptions.UnauthenticatedUserException;
import pl.coolture.restapi.common.pagination.CursorCodec;
import pl.coolture.restapi.common.pagination.CursorPayload;
import pl.coolture.restapi.participation.application.ParticipationService;
import pl.coolture.restapi.participation.domain.ParticipationType;
import pl.coolture.restapi.post.api.PostMapper;
import pl.coolture.restapi.post.api.dto.PostCardDto;
import pl.coolture.restapi.post.domain.Post;
import pl.coolture.restapi.post.domain.PostRepository;
import pl.coolture.restapi.post.domain.PostVisibility;
import pl.coolture.restapi.reaction.application.ReactionService;
import pl.coolture.restapi.reaction.domain.ReactionType;
import pl.coolture.restapi.user.application.UserAvatarService;
import pl.coolture.restapi.user.domain.User;

@ExtendWith(MockitoExtension.class)
class PostServiceGetFeedTest {
    @Mock private PostRepository          postRepository;
    @Mock private PostMapper              postMapper;
    @Mock private CursorCodec             cursorCodec;
    @Mock private ReactionService         reactionService;
    @Mock private ParticipationService    participationService;
    @Mock private UserAvatarService       userAvatarService;


    @InjectMocks
    private PostService postService;

    private static PostFeedFilters emptyFilters() {
        return PostFeedFilters.builder()
                .build();
    }

    private static PostFeedFilters filtersWithParticipation(List<String> types) {
        return PostFeedFilters.builder()
                .participationTypes(types)
                .build();
    }

    private static PostFeedFilters filtersWithReaction(String reactionType) {
        return PostFeedFilters.builder()
                .reactionType(reactionType)
                .build();
    }

    private static PostFeedFilters filtersWithQ(String q) {
        return PostFeedFilters.builder()
                .q(q)
                .build();
    }

    private static PostFeedFilters filtersWithTags(List<String> tags) {
        return PostFeedFilters.builder()
                .tags(tags)
                .build();
    }

    private static PostFeedFilters filtersWithRadius(Double radiusKm) {
        // lat/lng kept non-null so the geo filter is meaningful
        return PostFeedFilters.builder()
                .latitude(-15.0)
                .longitude(62.5)
                .radiusKm(radiusKm)
                .build();
    }

    /** Minimal PostCardDto with an id and createdAt, two fields CursorPage needs. */
    private static PostCardDto cardDto(UUID id, Instant createdAt) {
        return PostCardDto.builder()
                .id(id)
                .createdAt(createdAt)
                .build();
    }

    /**
     * Stubs postRepository.findFeed returns one mock Post per dto,
     * then stubs postMapper.toCard for each pair.
     * Call this helper first, then layer extra stubs on top.
     */
    private List<Post> stubRepoReturning(List<PostCardDto> dtos) {
        User author = mock(User.class);
        when(author.getId()).thenReturn(UUID.randomUUID());

        List<Post> posts = dtos.stream().map(d -> {
            Post p = mock(Post.class);
            when(p.getAuthor()).thenReturn(author);
            return p;
        }).toList();

        when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(posts);

        when(userAvatarService.resolveThumbnails(anyList())).thenReturn(Map.of());

        for (int i = 0; i < posts.size(); i++) {
            when(postMapper.toCard(eq(posts.get(i)), any())).thenReturn(dtos.get(i));
        }

        return posts;
    }

    /** Shared shortcut: stub cursorCodec.decode(null) → empty. */
    private void stubNoCursor() {
        when(cursorCodec.decode(null)).thenReturn(Optional.empty());
    }

    @Nested
    class ParticipationFilterValidation {

        @Test
        void nullCallerId_withParticipationTypes_throwsUnauthenticated() {
            var filters = filtersWithParticipation(List.of("INTERESTED"));

            assertThatThrownBy(() -> postService.getFeed(null, filters, null, 10))
                    .isInstanceOf(UnauthenticatedUserException.class);

            verifyNoInteractions(postRepository);
        }

        @Test
        void unknownParticipationType_throwsBadRequestContainingBadValue() {
            var filters = filtersWithParticipation(List.of("unknown_type"));

            assertThatThrownBy(() -> postService.getFeed(UUID.randomUUID(), filters, null, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("unknown_type");

            verifyNoInteractions(postRepository);
        }

        @Test
        void mixedValidAndUnknownTypes_throwsBadRequestListingOnlyUnknownOnes() {
            var filters = filtersWithParticipation(List.of("INTERESTED", "bad_type"));

            assertThatThrownBy(() -> postService.getFeed(UUID.randomUUID(), filters, null, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unknown participation type(s): [bad_type]")
                    .hasMessageContaining("Allowed values:");
        }

        @Test
        void nullParticipationTypes_andNullCallerId_doesNotValidate() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            assertThatCode(() -> postService.getFeed(null, emptyFilters(), null, 10))
                    .doesNotThrowAnyException();
        }

        @Test
        void emptyParticipationTypes_andNullCallerId_proceedsWithoutException() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            assertThatCode(() -> postService.getFeed(null, filtersWithParticipation(List.of()), null, 10))
                    .doesNotThrowAnyException();
        }

        @Test
        void allValidParticipationTypes_andAuthenticatedCaller_proceedsWithoutException() {
            UUID callerId = UUID.randomUUID();
            var filters = filtersWithParticipation(List.of("INTERESTED", "TAKES_PART"));

            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            assertThatCode(() -> postService.getFeed(callerId, filters, null, 10))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ReactionFilterValidation {

        @Test
        void nullCallerId_withReactionType_throwsUnauthenticated() {
            var filters = filtersWithReaction("LIKE");

            assertThatThrownBy(() -> postService.getFeed(null, filters, null, 10))
                    .isInstanceOf(UnauthenticatedUserException.class);

            verifyNoInteractions(postRepository);
        }

        @Test
        void unknownReactionType_throwsBadRequestContainingBadValue() {
            var filters = filtersWithReaction("love");

            assertThatThrownBy(() -> postService.getFeed(UUID.randomUUID(), filters, null, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("love")
                    .hasMessageContaining("Allowed values:");

            verifyNoInteractions(postRepository);
        }

        @Test
        void nullReactionType_andNullCallerId_doesNotValidate() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            assertThatCode(() -> postService.getFeed(null, emptyFilters(), null, 10))
                    .doesNotThrowAnyException();
        }

        @Test
        void validReactionType_andAuthenticatedCaller_proceedsWithoutException() {
            UUID callerId = UUID.randomUUID();
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            assertThatCode(() -> postService.getFeed(callerId, filtersWithReaction("LIKE"), null, 10))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class CursorDecoding {

        @Test
        void nullCursor_passesNullCursorFieldsToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, emptyFilters(), null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    isNull(),   // cursorCreatedAt
                    isNull(),   // cursorId
                    anyInt());
        }

        @Test
        void validCursor_extractsCreatedAtAndIdAndPassesThemToRepo() {
            UUID      cursorId = UUID.randomUUID();
            Instant   cursorTs = Instant.parse("2024-06-01T12:00:00Z");
            var       payload  = new CursorPayload(cursorId, cursorTs);

            when(cursorCodec.decode("valid-cursor")).thenReturn(Optional.of(payload));
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, emptyFilters(), "valid-cursor", 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    eq(cursorTs),   // cursorCreatedAt
                    eq(cursorId),   // cursorId
                    any(), anyInt());
        }

        @Test
        void malformedCursor_treatsAsFirstPageByPassingNullCursorFields() {
            // CursorCodec.decode contract: bad cursor => Optional.empty()
            when(cursorCodec.decode("garbled###")).thenReturn(Optional.empty());
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, emptyFilters(), "garbled###", 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    isNull(),
                    isNull(),
                    anyInt());
        }
    }

    @Nested
    class ArgumentTransformation {

        @Test
        void blankQuery_isPassedAsNullToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, filtersWithQ("   "), null, 10);

            verify(postRepository).findFeed(
                    isNull(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void nonBlankQuery_isPassedVerbatimToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, filtersWithQ("spring boot"), null, 10);

            verify(postRepository).findFeed(
                    eq("spring boot"), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void nullTagsList_passesNullTagsArrayToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, filtersWithTags(null), null, 10);

            verify(postRepository).findFeed(
                    any(),
                    isNull(),  // tags array
                    any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void emptyTagsList_passesNullTagsArrayToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, filtersWithTags(List.of()), null, 10);

            verify(postRepository).findFeed(
                    any(),
                    isNull(),  // empty list → null, not empty array
                    any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void nonEmptyTagsList_passesTagsArrayToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, filtersWithTags(List.of("music", "outdoor")), null, 10);

            verify(postRepository).findFeed(
                    any(),
                    argThat(arr -> Arrays.equals(arr, new String[]{"music", "outdoor"})),
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void nullRadiusKm_passesNullRadiusMetersToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, filtersWithRadius(null), null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(),
                    isNull(),  // radiusMeters
                    any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void radiusKm_isMultipliedBy1000BeforePassingToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, filtersWithRadius(5.0), null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(),
                    eq(5_000.0),  // 5 km → 5000 m
                    any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void limitPlusOneIsPassedToRepo_soHasMoreCanBeDetected() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            int limit = 20;

            postService.getFeed(null, emptyFilters(), null, limit);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    eq(limit + 1));
        }

        @Test
        void nullReactionType_passesNullToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, emptyFilters(), null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(),
                    isNull(),  // reactionType
                    any(), any(), any(),  any(), anyInt());
        }

        @Test
        void reactionType_isPassedVerbatimToRepo() {
            UUID callerId = UUID.randomUUID();
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(callerId, filtersWithReaction("DISLIKE"), null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(),
                    eq("DISLIKE"),  // reactionType
                    any(), any(), any(), any(), anyInt());
        }
    }

    @Nested
    class Enrichment {

        @Test
        void nullCallerId_skipsReactionAndParticipationLookupEntirely() {
            stubNoCursor();
            stubRepoReturning(List.of(cardDto(UUID.randomUUID(), Instant.now())));

            postService.getFeed(null, emptyFilters(), null, 10);

            verifyNoInteractions(reactionService, participationService);
        }

        @Test
        void nullCallerId_leavesMyReactionAndMyParticipationNull() {
            UUID id  = UUID.randomUUID();
            var  dto = cardDto(id, Instant.now());

            stubNoCursor();
            stubRepoReturning(List.of(dto));

            var page = postService.getFeed(null, emptyFilters(), null, 10);

            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().getMyReaction()).isNull();
            assertThat(page.items().getFirst().getMyParticipation()).isNull();
        }

        @Test
        void authenticatedCaller_withEmptyResults_skipsEnrichmentLookup() {
            UUID callerId = UUID.randomUUID();
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(callerId, emptyFilters(), null, 10);

            verifyNoInteractions(reactionService, participationService);
        }

        @Test
        void authenticatedCaller_setsMyReactionFromReactionService() {
            UUID callerId = UUID.randomUUID();
            UUID postId   = UUID.randomUUID();
            var  dto      = cardDto(postId, Instant.now());

            stubNoCursor();
            stubRepoReturning(List.of(dto));
            when(reactionService.findReactionTypesForPosts(eq(callerId), eq(List.of(postId))))
                    .thenReturn(Map.of(postId, ReactionType.LIKE));
            when(participationService.findParticipationTypesForPosts(any(), any()))
                    .thenReturn(Map.of());

            var page = postService.getFeed(callerId, emptyFilters(), null, 10);

            assertThat(page.items().getFirst().getMyReaction()).isEqualTo(ReactionType.LIKE);
        }

        @Test
        void authenticatedCaller_setsMyParticipationFromParticipationService() {
            UUID callerId = UUID.randomUUID();
            UUID postId   = UUID.randomUUID();
            var  dto      = cardDto(postId, Instant.now());

            stubNoCursor();
            stubRepoReturning(List.of(dto));
            when(reactionService.findReactionTypesForPosts(any(), any())).thenReturn(Map.of());
            when(participationService.findParticipationTypesForPosts(eq(callerId), eq(List.of(postId))))
                    .thenReturn(Map.of(postId, ParticipationType.TAKES_PART));

            var page = postService.getFeed(callerId, emptyFilters(), null, 10);

            assertThat(page.items().getFirst().getMyParticipation()).isEqualTo(ParticipationType.TAKES_PART);
        }

        @Test
        void authenticatedCaller_postWithNoMatchInMaps_fieldsRemainNull() {
            UUID callerId = UUID.randomUUID();
            var  dto      = cardDto(UUID.randomUUID(), Instant.now());

            stubNoCursor();
            stubRepoReturning(List.of(dto));
            when(reactionService.findReactionTypesForPosts(any(), any())).thenReturn(Map.of());
            when(participationService.findParticipationTypesForPosts(any(), any())).thenReturn(Map.of());

            var page = postService.getFeed(callerId, emptyFilters(), null, 10);

            assertThat(page.items().getFirst().getMyReaction()).isNull();
            assertThat(page.items().getFirst().getMyParticipation()).isNull();
        }

        @Test
        void enrichment_issuedAsSingleBatchCallForAllPostsOnThePage() {
            UUID callerId = UUID.randomUUID();
            UUID postId1  = UUID.randomUUID();
            UUID postId2  = UUID.randomUUID();

            stubNoCursor();
            stubRepoReturning(List.of(
                    cardDto(postId1, Instant.now()),
                    cardDto(postId2, Instant.now().minusSeconds(1))));
            when(reactionService.findReactionTypesForPosts(any(), any())).thenReturn(Map.of());
            when(participationService.findParticipationTypesForPosts(any(), any())).thenReturn(Map.of());

            postService.getFeed(callerId, emptyFilters(), null, 10);

            // Both IDs must arrive in 1 call, not two separate calls per post.
            verify(reactionService, times(1)).findReactionTypesForPosts(
                    eq(callerId),
                    argThat(ids -> ids.size() == 2
                            && ids.containsAll(List.of(postId1, postId2))));
            verify(participationService, times(1)).findParticipationTypesForPosts(
                    eq(callerId),
                    argThat(ids -> ids.size() == 2
                            && ids.containsAll(List.of(postId1, postId2))));
        }
    }

    @Nested
    class VisibilityFilter {

        @Test
        void nullCallerId_noExplicitVisibility_passesPublicToRepo() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(null, emptyFilters(), null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(),
                    eq("PUBLIC"),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void authenticatedCaller_noExplicitVisibility_passesNullToRepo() {
            UUID callerId = UUID.randomUUID();
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            postService.getFeed(callerId, emptyFilters(), null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(),
                    isNull(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
        }

        @Test
        void explicitVisibility_isPassedVerbatimRegardlessOfAuth() {
            stubNoCursor();
            when(postRepository.findFeed(any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            var filters = PostFeedFilters.builder().visibility(PostVisibility.FRIENDS).build();
            postService.getFeed(null, filters, null, 10);

            verify(postRepository).findFeed(
                    any(), any(), any(), any(),
                    eq("FRIENDS"),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
        }
    }

    @Nested
class SortByTransformation {

    private static PostFeedFilters filtersWithSortBy(String sortBy) {
        return PostFeedFilters.builder().sortBy(sortBy).build();
    }

    @Test
    void nullSortBy_passesNullToRepo() {
        stubNoCursor();
        when(postRepository.findFeed(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        postService.getFeed(null, emptyFilters(), null, 10);

        verify(postRepository).findFeed(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                isNull(),   // sortBy — param #17 (1-indexed), 16 any()s before it
                anyInt());
    }

    @Test
    void blankSortBy_isPassedAsNullToRepo() {
        stubNoCursor();
        when(postRepository.findFeed(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        postService.getFeed(null, filtersWithSortBy("   "), null, 10);

        verify(postRepository).findFeed(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                isNull(),
                anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECENT", "POPULAR", "UPCOMING"})
    void validSortBy_isPassedVerbatimToRepo(String sortBy) {
        stubNoCursor();
        when(postRepository.findFeed(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        postService.getFeed(null, filtersWithSortBy(sortBy), null, 10);

        verify(postRepository).findFeed(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                eq(sortBy),
                anyInt());
    }
}
}