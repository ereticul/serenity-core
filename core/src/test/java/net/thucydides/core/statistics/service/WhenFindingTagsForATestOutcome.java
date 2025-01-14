package net.thucydides.core.statistics.service;

import com.google.common.base.Optional;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTagValuesOf;
import net.thucydides.core.annotations.WithTags;
import net.thucydides.core.model.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestTag;
import net.thucydides.core.requirements.FileSystemRequirementsTagProvider;
import net.thucydides.core.requirements.model.Narrative;
import net.thucydides.core.requirements.model.Requirement;
import net.thucydides.core.requirements.model.cucumber.CucumberParser;
import net.thucydides.core.util.MockEnvironmentVariables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

public class WhenFindingTagsForATestOutcome {

    @Mock
    TestOutcome emptyTestOutcome;

    MockEnvironmentVariables environmentVariables;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        environmentVariables = new MockEnvironmentVariables();
    }

    @Test
    public void should_find_the_annotation_tag_provider_by_default() {
        TagProviderService tagProviderService = new ClasspathTagProviderService();
        List<TagProvider> tagProviders = tagProviderService.getTagProviders();

        boolean containsAnnotationTagProvider = false;
        for(TagProvider provider : tagProviders) {
            if (provider instanceof AnnotationBasedTagProvider) {
                containsAnnotationTagProvider = true;
            }
        }
        assertThat(containsAnnotationTagProvider, is(true));
    }

    @Test
    public void should_also_find_the_file_system_requirements_provider_by_default() {
        TagProviderService tagProviderService = new ClasspathTagProviderService();
        List<TagProvider> tagProviders = tagProviderService.getTagProviders();

        boolean containsRequirementsProvider = false;
        for(TagProvider provider : tagProviders) {
            if (provider instanceof FileSystemRequirementsTagProvider) {
                containsRequirementsProvider = true;
            }
        }
        assertThat(containsRequirementsProvider, is(true));
    }

    @Test
    public void annotation_based_tag_should_return_no_tags_if_the_test_class_is_not_defined() {
        when(emptyTestOutcome.getTestCase()).thenReturn(null);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        assertThat(tagProvider.getTagsFor(emptyTestOutcome).size(), is(0));
    }

    class SomeUnannotatedTestCase {
        public void some_test_method() {}
    }

    @Test
    public void annotation_based_tag_should_return_no_annotated_tags_if_no_tags_present_in_the_test_class() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeUnannotatedTestCase.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags.size(), is(0));
    }

    @WithTag(name="Car sales", type="pillar")
    class SomeTestCase {
        public void some_test_method() {}
    }

    @Test
    public void annotation_based_tag_should_return_annotated_tags_if_tags_present_in_the_test_class() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCase.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags.size(), is(not(0)));
        TestTag tag = tags.iterator().next();

        assertThat(tag.getName(), is("Car sales"));
        assertThat(tag.getType(), is("pillar"));
    }

    class SomeTestCaseWithTagOnMethod {
        @WithTag(name="Car sales", type="pillar")
        public void some_test_method() {}
    }

    @Test
    public void annotation_based_tag_should_return_annotated_tags_if_tags_present_on_the_test_method() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCaseWithTagOnMethod.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags.size(), is(not(0)));
        TestTag tag = tags.iterator().next();

        assertThat(tag.getName(), is("Car sales"));
        assertThat(tag.getType(), is("pillar"));
    }

    @WithTag(name="More Car sales", type="pillar")
    class SomeTestCaseWithTagOnMethodAndClass {
        @WithTag(name="Car sales", type="pillar")
        public void some_test_method() {}
    }

    @Test
    public void annotation_based_tag_should_return_annotated_tags_from_the_class_and_the_method() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCaseWithTagOnMethodAndClass.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags.size(), is(2));
    }


    class SomeTestCaseWithAShortenedTagOnAMethod {
        @WithTag("pillar:Car sales")
        public void some_test_method() {}
    }

    @Test
    public void tags_can_use_a_shorthand_notation() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCaseWithAShortenedTagOnAMethod.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        TestTag tag = (TestTag) tags.toArray()[0];
        assertThat(tag.getName(), is("Car sales"));
        assertThat(tag.getType(), is("pillar"));
    }

    class SomeTestCaseWithSeveralShortenedaTagOnAMethod {
        @WithTagValuesOf({"pillar: car sales", "A tag"})
        public void some_test_method() {}
    }

    @Test
    public void multiple_tags_can_use_a_shorthand_notation() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCaseWithSeveralShortenedaTagOnAMethod.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags, hasItem(TestTag.withName("A tag").andType("tag")));
        assertThat(tags, hasItem(TestTag.withName("car sales").andType("pillar")));
    }


    @WithTags(
            {
                    @WithTag(name="Car sales", type="pillar"),
                    @WithTag(name="Boat sales", type="pillar")
            }
    )
    class SomeTestCaseWithTagsOnClass {
        public void some_test_method() {}
    }

    @Test
    public void annotation_based_tags_should_return_multiple_annotated_tags_if_tags_present_on_the_test_class() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCaseWithTagsOnClass.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags.size(), is(2));
    }

    class SomeTestCaseWithTagsOnMethod {
        @WithTags(
                {
                        @WithTag(name="Car sales", type="pillar"),
                        @WithTag(name="Boat sales", type="pillar")
                }
        )
        public void some_test_method() {}
    }

    @Test
    public void annotation_based_tags_should_return_multiple_annotated_tags_if_tags_present_on_the_test_method() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCaseWithTagsOnMethod.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags.size(), is(2));
    }

    @WithTag(name="Online sales", type="capability")
    class SomeTestCaseWithTagsOnMethodAndClass {
        @WithTags(
                {
                        @WithTag(name="Car sales", type="pillar"),
                        @WithTag(name="Boat sales", type="pillar")
                }
        )
        public void some_test_method() {}
    }

    @Test
    public void annotation_based_tags_should_return_multiple_annotated_tags_if_tags_present_on_the_test_method_and_class() {

        TestOutcome testOutcome = TestOutcome.forTest("some_test_method", SomeTestCaseWithTagsOnMethodAndClass.class);

        AnnotationBasedTagProvider tagProvider = new AnnotationBasedTagProvider();

        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags.size(), is(3));
    }

    @Mock TestOutcome testOutcome;

    @Test
    public void should_get_tags_from_story_path() {
        FileSystemRequirementsTagProvider tagProvider = new FileSystemRequirementsTagProvider();

        when(testOutcome.getPath()).thenReturn("stories.grow_potatoes.grow_new_potatoes.PlantPotatoes");
        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes").andType("capability")));
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes/Grow new potatoes").andType("feature")));
        assertThat(tags, hasItem(TestTag.withName("Grow new potatoes/Plant potatoes").andType("story")));
    }

    @Test
    public void should_get_tags_from_story_path_with_file_separators() {
        FileSystemRequirementsTagProvider tagProvider = new FileSystemRequirementsTagProvider();

        when(testOutcome.getPath()).thenReturn("stories/grow_potatoes/grow_new_potatoes/PlantNewPotatoes");
        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes").andType("capability")));
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes/Grow new potatoes").andType("feature")));
    }

    @Test
    public void should_ignore_dot_story_suffix_in_path() {
        FileSystemRequirementsTagProvider tagProvider = new FileSystemRequirementsTagProvider();

        when(testOutcome.getPath()).thenReturn("stories/grow_potatoes/grow_new_potatoes/PlantPotatoes.story");
        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes").andType("capability")));
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes/Grow new potatoes").andType("feature")));
        assertThat(tags, hasItem(TestTag.withName("Grow new potatoes/Plant potatoes").andType("story")));
    }

    @Test
    public void should_get_tags_from_story_path_with_windows_file_separators() {
        FileSystemRequirementsTagProvider tagProvider = new FileSystemRequirementsTagProvider();

        when(testOutcome.getPath()).thenReturn("stories\\grow_potatoes\\grow_new_potatoes\\PlantNewPotatoes");
        Set<TestTag> tags = tagProvider.getTagsFor(testOutcome);
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes").andType("capability")));
        assertThat(tags, hasItem(TestTag.withName("Grow potatoes/Grow new potatoes").andType("feature")));
    }

    @Test
         public void should_get_requirement_from_story_with_narrative_if_present() {
        FileSystemRequirementsTagProvider tagProvider = new FileSystemRequirementsTagProvider();
        Story userStory = Story.called("plant potatoes");
        when(testOutcome.getPath()).thenReturn("stories\\grow_potatoes\\grow_new_potatoes\\PlantPotatoes.story");
        when(testOutcome.getUserStory()).thenReturn(userStory);

        Optional<Requirement> requirement = tagProvider.getParentRequirementOf(testOutcome);

        assertThat(requirement.isPresent(), is(true));
        assertThat(requirement.get().getName(), is("Plant potatoes"));
        assertThat(requirement.get().getNarrative().getText(), containsString("As a farmer"));
        assertThat(requirement.get().getNarrative().getText(), containsString("I want to plant potatoes"));
        assertThat(requirement.get().getNarrative().getText(), containsString("So that I can harvest them later on"));
    }

    @Test
    public void should_get_requirement_from_feature_with_narrative_if_present() {
        FileSystemRequirementsTagProvider tagProvider = new FileSystemRequirementsTagProvider();
        Story userStory = Story.called("plant potatoes");
        when(testOutcome.getPath()).thenReturn("stories\\grow_potatoes\\grow_new_potatoes\\PlantPotatoes.feature");
        when(testOutcome.getUserStory()).thenReturn(userStory);

        Optional<Requirement> requirement = tagProvider.getParentRequirementOf(testOutcome);

        assertThat(requirement.isPresent(), is(true));
        assertThat(requirement.get().getName(), is("Plant potatoes"));
        assertThat(requirement.get().getNarrative().getText(), containsString("As a farmer"));
        assertThat(requirement.get().getNarrative().getText(), containsString("I want to plant potatoes"));
        assertThat(requirement.get().getNarrative().getText(), containsString("So that I can harvest them later on"));
    }

    @Test
    public void should_get_requirement_from_feature_in_a_foreign_language() throws URISyntaxException {

        environmentVariables.setProperty("feature.file.language","no"); // Norweigan
        CucumberParser parser = new CucumberParser(environmentVariables);

        URL url = this.getClass().getResource("/features/PlantScandanavianPotatoes.feature");
        File featureFile = new File(url.toURI());

        Optional<Narrative> narrative = parser.loadFeatureNarrative(featureFile);

        assertThat(narrative.isPresent(), is(true));
    }

}

