package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryRepository;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryTag;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabContainerRegistryTagDetails;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class RegistryJsonTest {

    private static final String REPOSITORY = """
            {"id":1,"name":"","path":"group/project","project_id":9,
            "location":"gitlab.example.com:5000/group/project","created_at":"2019-01-10T13:39:08.229Z",
            "cleanup_policy_started_at":"2020-01-10T15:40:57.392Z","tags_count":1,
            "tags":[{"name":"0.0.1","path":"group/project:0.0.1",
            "location":"gitlab.example.com:5000/group/project:0.0.1"}],
            "delete_api_path":"https://gitlab.example.com/api/v4/projects/9/registry/repositories/1",
            "size":2818413,"status":"delete_scheduled"}""";

    private static final String TAG = """
            {"name":"0.0.1","path":"group/project:0.0.1",
            "location":"gitlab.example.com:5000/group/project:0.0.1"}""";

    private static final String DETAILS = """
            {"name":"v10.0.0","path":"group/project:v10.0.0",
            "location":"gitlab.example.com:5000/group/project:v10.0.0",
            "revision":"e9ed9d8a8b7f2b9f6a9c9d44ab0f4a5c14d8f53a1e0d0a8a1b7c8a35c4e1d9f2",
            "short_revision":"e9ed9d8a8","digest":"sha256:1b8a97e2a5a3d1f1e4b4b0b0f2b3c4d5e6f708192a3b4c5d6e7f8091a2b3c4d5",
            "created_at":"2019-01-06T16:49:51.272+00:00","total_size":1099511627776}""";

    private final GitLabSdk sdk = GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS));

    private static <T> T decode(final String json, final Class<T> type) {
        return GitLabJsonCodecs.defaults().decode(json.getBytes(StandardCharsets.UTF_8), type);
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenRepositoryIsFull() {
        final RegistryRepository sut =
                new RegistryRepository(sdk, decode(REPOSITORY, GitLabContainerRegistryRepository.class));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(1L);
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.path()).isEqualTo("group/project");
            softly.assertThat(sut.location()).isEqualTo("gitlab.example.com:5000/group/project");
            softly.assertThat(sut.tagsCount()).contains(1);
            softly.assertThat(sut.response().projectId()).isEqualTo(9L);
            softly.assertThat(sut.response().createdAt()).isEqualTo(Instant.parse("2019-01-10T13:39:08.229Z"));
            softly.assertThat(sut.response().cleanupPolicyStartedAt())
                    .isEqualTo(Instant.parse("2020-01-10T15:40:57.392Z"));
            softly.assertThat(sut.response().tags())
                    .singleElement()
                    .satisfies(tag -> softly.assertThat(tag.name()).isEqualTo("0.0.1"));
            softly.assertThat(sut.response().size()).isEqualTo(2_818_413L);
            softly.assertThat(sut.response().status()).isEqualTo("delete_scheduled");
            softly.assertThat(sut.response().deleteApiPath()).endsWith("/registry/repositories/1");
        });
    }

    @Test
    void decode_shouldReadRealShapedJson_whenListedTagIsSmaller() {
        final GitLabContainerRegistryTag tag = decode(TAG, GitLabContainerRegistryTag.class);

        assertSoftly(softly -> {
            softly.assertThat(tag.name()).isEqualTo("0.0.1");
            softly.assertThat(tag.path()).isEqualTo("group/project:0.0.1");
            softly.assertThat(tag.location()).isEqualTo("gitlab.example.com:5000/group/project:0.0.1");
        });
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenTagDetailsAreFull() {
        final RegistryRepository repository =
                new RegistryRepository(sdk, decode(REPOSITORY, GitLabContainerRegistryRepository.class));

        final RegistryTag sut = new RegistryTag(repository, decode(DETAILS, GitLabContainerRegistryTagDetails.class));

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("v10.0.0");
            softly.assertThat(sut.location()).isEqualTo("gitlab.example.com:5000/group/project:v10.0.0");
            softly.assertThat(sut.digest())
                    .hasValueSatisfying(digest -> softly.assertThat(digest).startsWith("sha256:"));
            softly.assertThat(sut.totalSize()).contains(1_099_511_627_776L);
            softly.assertThat(sut.response().shortRevision()).isEqualTo("e9ed9d8a8");
            softly.assertThat(sut.response().createdAt()).isEqualTo(Instant.parse("2019-01-06T16:49:51.272Z"));
        });
    }
}
