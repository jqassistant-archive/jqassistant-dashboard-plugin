package org.jqassistant.contrib.plugin.dashboard;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.Concept;
import com.buschmais.jqassistant.core.analysis.api.rule.RuleException;
import com.buschmais.jqassistant.plugin.java.api.model.ClassFileDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.PackageDescriptor;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitAuthorDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import org.junit.Test;

import java.util.Map;

import static com.buschmais.jqassistant.core.analysis.api.Result.Status.SUCCESS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DashboardIT extends AbstractJavaPluginIT {

    /**
     * Verifies that the concept "dashboard:Authors" is successful, if it is applicable.
     */
    @Test
    public void validAuthors() throws RuleException {
        store.beginTransaction();
        GitAuthorDescriptor author1 = store.create(GitAuthorDescriptor.class);
        author1.setName("Bob");
        author1.setEmail("bob1@example.com");
        author1.setIdentString("1");
        GitAuthorDescriptor author2 = store.create(GitAuthorDescriptor.class);
        author2.setName("Bob");
        author2.setEmail("bob2@example.com");
        author2.setIdentString("2");
        GitCommitDescriptor commit1 = store.create(GitCommitDescriptor.class);
        commit1.setAuthor("1");
        commit1.setCommitter("1");
        author1.getCommits().add(commit1);
        GitCommitDescriptor commit2 = store.create(GitCommitDescriptor.class);
        commit2.setAuthor("2");
        commit2.setCommitter("2");
        author2.getCommits().add(commit2);
        store.commitTransaction();

        Result<Concept> result = applyConcept("dashboard:Authors");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));

        store.reset();
    }

    /**
     * Verifies that the concept "dashboard:Merge" is successful, if it is applicable.
     */
    @Test
    public void validMerge() throws RuleException {
        Result<Concept> result = applyConcept("dashboard:Merge");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat(result.getRows().get(0).containsKey("count(c)"), equalTo(true));
        assertThat((Long) result.getRows().get(0).get("count(c)"), equalTo(0L));
    }

    /**
     * Verifies that the concept "dashboard:Timetree" is successful, if it is applicable.
     */
    @Test
    public void validTimetree() throws RuleException {
        store.beginTransaction();
        GitCommitDescriptor commit = store.create(GitCommitDescriptor.class);
        commit.setDate("2077-10-22");
        store.commitTransaction();

        Result<Concept> result = applyConcept("dashboard:Timetree");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat(result.getRows().get(0).get("y"), notNullValue());
        assertThat(result.getRows().get(0).get("m"), notNullValue());
        assertThat(result.getRows().get(0).get("d"), notNullValue());

        store.reset();
    }

    /**
     * Verifies that the concept "dashboard:GitFileName" is successful, if it is applicable.
     */
    @Test
    public void validGitFileName() throws RuleException {
        store.beginTransaction();
        GitFileDescriptor file1 = store.create(GitFileDescriptor.class);
        file1.setRelativePath("file1.txt");
        GitFileDescriptor file2 = store.create(GitFileDescriptor.class);
        file2.setRelativePath("file2.config");
        store.commitTransaction();

        Result<Concept> result = applyConcept("dashboard:GitFileName");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat((Long) result.getRows().get(0).get("count(f)"), equalTo(2L));

        store.reset();
    }

    /**
     * Verifies that the concept "dashboard:TypeHasSourceGitFile" is successful, if it is applicable.
     */
    @Test
    public void validTypeHasSourceGitFile() throws RuleException {
        store.beginTransaction();
        ClassFileDescriptor file = store.create(ClassFileDescriptor.class);
        file.setSourceFileName("Test.java");
        PackageDescriptor pack = store.create(PackageDescriptor.class);
        pack.setFileName("/package");
        pack.getContains().add(file);
        GitFileDescriptor gitFile = store.create(GitFileDescriptor.class);
        gitFile.setRelativePath("./package/Test.java");
        store.commitTransaction();

        Result<Concept> result = applyConcept("dashboard:TypeHasSourceGitFile");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat(result.getRows().get(0), notNullValue());

        store.reset();
    }

    /**
     * Verifies that the concept "dashboard:Filetype" is successful, if it is applicable.
     */
    @Test
    public void validFiletype() throws RuleException {
        store.beginTransaction();
        GitFileDescriptor file1 = store.create(GitFileDescriptor.class);
        file1.setRelativePath("file1.txt");
        store.commitTransaction();

        Result<Concept> result = applyConcept("dashboard:Filetype");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat((String) result.getRows().get(0).get("filetype"), equalTo("txt"));

        store.reset();
    }

    /**
     * Verifies the group "dashboard:Default".
     */
    @Test
    public void defaultGroup() throws RuleException {
        executeGroup("dashboard:Default");
        Map<String, Result<Concept>> result = reportPlugin.getConceptResults();
        assertThat(result.isEmpty(), equalTo(false));
        assertThat(result.get("dashboard:Authors"), notNullValue());
        assertThat(result.get("dashboard:Merge"), notNullValue());
        assertThat(result.get("dashboard:Timetree"), notNullValue());
        assertThat(result.get("dashboard:GitFileName"), notNullValue());
        assertThat(result.get("dashboard:TypeHasSourceGitFile"), notNullValue());
        assertThat(result.get("dashboard:Filetype"), notNullValue());
    }
}
