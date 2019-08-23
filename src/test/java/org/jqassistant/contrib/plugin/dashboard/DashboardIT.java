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

import java.util.List;
import java.util.Map;

import static com.buschmais.jqassistant.core.analysis.api.Result.Status.SUCCESS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DashboardIT extends AbstractJavaPluginIT {

    /**
     * Verifies that the concept "jqassistant-dashboard:GitDuplicateAuthorsByName" is successful, if it is applicable.
     */
    @Test
    public void validGitDuplicateAuthorsByName() throws RuleException {
        store.beginTransaction();
        GitAuthorDescriptor author1 = store.create(GitAuthorDescriptor.class);
        author1.setName("Bob");
        author1.setEmail("bob1@example.com");
        GitAuthorDescriptor author2 = store.create(GitAuthorDescriptor.class);
        author2.setName("Bob");
        author2.setEmail("bob2@example.com");
        initCommits(author1, author2);
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:GitDuplicateAuthorsByName");
        testGitDuplicateAuthors(result);
    }

    /**
     * Verifies that the concept "jqassistant-dashboard:GitDuplicateAuthorsByEmail" is successful, if it is applicable.
     */
    @Test
    public void validGitDuplicateAuthorsByEmail() throws RuleException {
        store.beginTransaction();
        GitAuthorDescriptor author1 = store.create(GitAuthorDescriptor.class);
        author1.setName("Alice");
        author1.setEmail("alice@example.com");
        GitAuthorDescriptor author2 = store.create(GitAuthorDescriptor.class);
        author2.setName("Al");
        author2.setEmail("alice@example.com");
        initCommits(author1, author2);
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:GitDuplicateAuthorsByEmail");
        testGitDuplicateAuthors(result);
    }

    private void initCommits(GitAuthorDescriptor author1, GitAuthorDescriptor author2) {
        GitCommitDescriptor commit1 = store.create(GitCommitDescriptor.class);
        author1.getCommits().add(commit1);
        GitCommitDescriptor commit2 = store.create(GitCommitDescriptor.class);
        author2.getCommits().add(commit2);
    }

    private void testGitDuplicateAuthors(Result<Concept> result) {
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat((Long) result.getRows().get(0).get("NumberOfDuplicates"), equalTo(1L));

        store.beginTransaction();
        TestResult testResult = query("MATCH (a:Author)-[:COMMITTED]->(c:Commit) RETURN a as Author, count(c) as Commits");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertThat(rows.size(), equalTo(1));
        assertThat(rows.get(0).containsKey("Commits"), equalTo(true));
        assertThat((Long) rows.get(0).get("Commits"), equalTo(2L));
    }

    /**
     * Verifies that the concept "jqassistant-dashboard:GitMergeCommit" is successful, if it is applicable.
     */
    @Test
    public void validGitMergeCommit() throws RuleException {
        store.beginTransaction();
        GitCommitDescriptor parent1 = store.create(GitCommitDescriptor.class);
        GitCommitDescriptor parent2 = store.create(GitCommitDescriptor.class);
        GitCommitDescriptor merge = store.create(GitCommitDescriptor.class);
        merge.getParents().add(parent1);
        merge.getParents().add(parent2);
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:GitMergeCommit");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat(result.getRows().get(0).containsKey("MergeCommits"), equalTo(true));
        assertThat((Long) result.getRows().get(0).get("MergeCommits"), equalTo(1L));

        store.beginTransaction();
        TestResult testResult = query("MATCH (c:Commit:Merge) RETURN count(c) as NumberOfMergeCommits");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertThat(rows.size(), equalTo(1));
        assertThat(rows.get(0).containsKey("NumberOfMergeCommits"), equalTo(true));
        assertThat((Long) rows.get(0).get("NumberOfMergeCommits"), equalTo(1L));
    }

    /**
     * Verifies that the concept "jqassistant-dashboard:GitTimeTree" is successful, if it is applicable.
     */
    @Test
    public void validGitTimeTree() throws RuleException {
        store.beginTransaction();
        GitCommitDescriptor commit = store.create(GitCommitDescriptor.class);
        commit.setDate("2077-10-22");
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:GitTimeTree");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));

        store.beginTransaction();
        TestResult testResult = query("MATCH (:Commit)-[:OF_DAY]->(d:Day)-[:OF_MONTH]-(m:Month)-[:OF_YEAR]->(y:Year) RETURN d.day as Day, m.month as Month, y.year as Year");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertThat(rows.size(), equalTo(1));
        assertThat((String) rows.get(0).get("Day"), equalTo("22"));
        assertThat((String) rows.get(0).get("Month"), equalTo("10"));
        assertThat((String) rows.get(0).get("Year"), equalTo("2077"));
    }

    /**
     * Verifies that the concept "jqassistant-dashboard:GitFileName" is successful, if it is applicable.
     */
    @Test
    public void validGitFileName() throws RuleException {
        store.beginTransaction();
        GitFileDescriptor file1 = store.create(GitFileDescriptor.class);
        file1.setRelativePath("file1.txt");
        GitFileDescriptor file2 = store.create(GitFileDescriptor.class);
        file2.setRelativePath("file2.config");
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:GitFileName");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat((Long) result.getRows().get(0).get("Files"), equalTo(2L));

        store.beginTransaction();
        TestResult testResult = query("MATCH (f:Git:File) RETURN f.fileName as FileName, f.relativePath as RelPath");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertThat(rows.size(), equalTo(2));
        assertThat(rows.get(0).get("FileName"), equalTo(rows.get(0).get("RelPath")));
    }


    /**
     * Verifies that the concept "jqassistant-dashboard:TypeHasSourceGitFile" is successful, if it is applicable.
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
        gitFile.setRelativePath("/src/main/java/package/Test.java");
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:TypeHasSourceGitFile");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat((Long) result.getRows().get(0).get("Matches"), equalTo(1L));

        store.beginTransaction();
        TestResult testResult = query("MATCH (t:Java:Type)-[h:HAS_SOURCE]->(f:Git:File) RETURN count(h) as RelationCount");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertThat(rows.size(), equalTo(1));
        assertThat((Long) rows.get(0).get("RelationCount"), equalTo(1L));

    }

    /**
     * Verifies that the concept "jqassistant-dashboard:FileType" is successful, if it is applicable.
     */
    @Test
    public void validFiletype() throws RuleException {
        store.beginTransaction();
        GitFileDescriptor file1 = store.create(GitFileDescriptor.class);
        file1.setRelativePath("test/file1.txt");
        GitFileDescriptor noFile = store.create(GitFileDescriptor.class);
        file1.setRelativePath("otherStuff/.txt");
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:FileType");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat((String) result.getRows().get(0).get("FileType"), equalTo("txt"));
        assertThat((Long) result.getRows().get(0).get("FilesOfType"), equalTo(1L));

        store.beginTransaction();
        TestResult testResult = query("MATCH (f:Git:File) WHERE EXISTS(f.type) RETURN count(f) as TypedFileCount");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertThat(rows.size(), equalTo(1));
        assertThat((Long) rows.get(0).get("TypedFileCount"), equalTo(1L));
    }

    /**
     * Verifies the group "jqassistant-dashboard:Default".
     */
    @Test
    public void defaultGroup() throws RuleException {
        executeGroup("jqassistant-dashboard:Default");
        Map<String, Result<Concept>> result = reportPlugin.getConceptResults();
        assertThat(result.isEmpty(), equalTo(false));
        assertThat(result.get("jqassistant-dashboard:GitDuplicateAuthorsByName"), notNullValue());
        assertThat(result.get("jqassistant-dashboard:GitDuplicateAuthorsByEmail"), notNullValue());
        assertThat(result.get("jqassistant-dashboard:GitMergeCommit"), notNullValue());
        assertThat(result.get("jqassistant-dashboard:GitTimeTree"), notNullValue());
        assertThat(result.get("jqassistant-dashboard:GitFileName"), notNullValue());
        assertThat(result.get("jqassistant-dashboard:TypeHasSourceGitFile"), notNullValue());
        assertThat(result.get("jqassistant-dashboard:FileType"), notNullValue());
    }
}
