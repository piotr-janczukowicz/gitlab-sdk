package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabPackage;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabPackageFile;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class PackagesJsonTest {

    private static final String PIPELINE = """
            {"id":123,"iid":1,"project_id":1,"sha":"b83d6e391c22777fca1ed3012fce84f633d7fed0",
            "ref":"main","status":"pending","source":"push","created_at":"2019-11-27T03:37:38.711Z",
            "updated_at":"2019-11-27T03:37:38.711Z","web_url":"https://gitlab.example.com/foo/bar/pipelines/123",
            "user":{"id":1,"username":"admin","name":"Admin","state":"active","avatar_url":null,
            "web_url":"https://gitlab.example.com/admin"}}""";

    private static final String PACKAGE = """
            {"id":1,"name":"com/mycompany/my-app","version":"1.0-SNAPSHOT","package_type":"maven",
            "status":"default",
            "_links":{"web_path":"/namespace1/project1/-/packages/1",
            "delete_api_path":"https://gitlab.example.com/api/v4/projects/1/packages/1"},
            "created_at":"2019-11-27T03:37:38.711Z","last_downloaded_at":"2019-12-01T10:00:00.000+00:00",
            "creator_id":2,"project_id":1,"project_path":"namespace1/project1",
            "tags":[{"id":9,"package_id":1,"name":"latest","created_at":"2019-11-27T03:37:38.711Z",
            "updated_at":"2019-11-27T03:37:38.711Z","project_id":1}],
            "pipeline":%1$s,"pipelines":[%1$s],
            "versions":[{"id":2,"version":"2.0-SNAPSHOT","created_at":"2020-04-28T04:42:11.573Z",
            "tags":[{"id":10,"package_id":2,"name":"next","created_at":"2020-04-28T04:42:11.573Z",
            "updated_at":"2020-04-28T04:42:11.573Z","project_id":1}],"pipeline":%1$s}]}""".formatted(PIPELINE);

    private static final String FILE = """
            {"id":25,"package_id":1,"created_at":"2018-11-07T15:25:52.199Z",
            "file_name":"my-app-1.5-20181107.152550-1.jar","size":2421,
            "file_md5":"58e6a45a629910c6ff99145a688971ac","file_sha1":"ebd193463d3915d7e22219f52740056dfd26cbfe",
            "file_sha256":"a903393463d3915d7e22219f52740056dfd26cbfe","pipelines":[%s]}""".formatted(PIPELINE);

    private final GitLabSdk sdk = GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS));

    @Test
    void accessors_shouldReadRealShapedJson_whenPackageIsFull() {
        final GitLabPackage decoded =
                GitLabJsonCodecs.defaults().decode(PACKAGE.getBytes(StandardCharsets.UTF_8), GitLabPackage.class);

        final ProjectPackage sut = new ProjectPackage(sdk, decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(1L);
            softly.assertThat(sut.name()).isEqualTo("com/mycompany/my-app");
            softly.assertThat(sut.version()).contains("1.0-SNAPSHOT");
            softly.assertThat(sut.type()).isEqualTo(PackageType.MAVEN);
            softly.assertThat(sut.status()).isEqualTo("default");
            softly.assertThat(sut.createdAt()).isEqualTo(Instant.parse("2019-11-27T03:37:38.711Z"));
            softly.assertThat(sut.lastDownloadedAt()).contains(Instant.parse("2019-12-01T10:00:00Z"));
            softly.assertThat(sut.response().links().webPath()).isEqualTo("/namespace1/project1/-/packages/1");
            softly.assertThat(sut.response().creatorId()).isEqualTo(2L);
            softly.assertThat(sut.response().projectId()).isEqualTo(1L);
            softly.assertThat(sut.response().projectPath()).isEqualTo("namespace1/project1");
            softly.assertThat(sut.response().tags()).singleElement().satisfies(tag -> {
                assertThat(tag.name()).isEqualTo("latest");
                assertThat(tag.packageId()).isEqualTo(1L);
            });
            softly.assertThat(sut.response().pipeline().id()).isEqualTo(123L);
            softly.assertThat(sut.response().pipelines()).hasSize(1);
            softly.assertThat(sut.response().versions()).singleElement().satisfies(version -> {
                assertThat(version.version()).isEqualTo("2.0-SNAPSHOT");
                assertThat(version.createdAt()).isEqualTo(Instant.parse("2020-04-28T04:42:11.573Z"));
                assertThat(version.tags()).hasSize(1);
            });
        });
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenFileIsFull() {
        final GitLabPackageFile decoded =
                GitLabJsonCodecs.defaults().decode(FILE.getBytes(StandardCharsets.UTF_8), GitLabPackageFile.class);

        final PackageFile sut = new PackageFile(new ProjectPackage(sdk, PackagesFixtures.pkg(1L)), decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(25L);
            softly.assertThat(sut.fileName()).isEqualTo("my-app-1.5-20181107.152550-1.jar");
            softly.assertThat(sut.size()).contains(2421L);
            softly.assertThat(sut.sha256()).contains("a903393463d3915d7e22219f52740056dfd26cbfe");
            softly.assertThat(sut.response().packageId()).isEqualTo(1L);
            softly.assertThat(sut.response().fileMd5()).isEqualTo("58e6a45a629910c6ff99145a688971ac");
            softly.assertThat(sut.response().pipelines()).hasSize(1);
        });
    }
}
