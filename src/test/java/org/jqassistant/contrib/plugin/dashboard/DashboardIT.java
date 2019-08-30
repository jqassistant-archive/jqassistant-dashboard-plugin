package org.jqassistant.contrib.plugin.dashboard;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.Concept;
import com.buschmais.jqassistant.core.analysis.api.rule.RuleException;
import com.buschmais.jqassistant.plugin.common.api.model.ArtifactFileDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.ClassFileDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.ClassTypeDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.PackageDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.TypeDescriptor;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitAuthorDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.buschmais.jqassistant.core.analysis.api.Result.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(SUCCESS, result.getStatus());
        assertEquals(1, result.getRows().size());
        assertEquals(1L, result.getRows().get(0).get("NumberOfDuplicates"));

        store.beginTransaction();
        TestResult testResult = query("MATCH (a:Author)-[:COMMITTED]->(c:Commit) RETURN a as Author, count(c) as Commits");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).containsKey("Commits"));
        assertEquals(2L, rows.get(0).get("Commits"));
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
        assertEquals(SUCCESS, result.getStatus());
        assertEquals(1, result.getRows().size());
        assertTrue(result.getRows().get(0).containsKey("MergeCommits"));
        assertEquals(1L, result.getRows().get(0).get("MergeCommits"));

        store.beginTransaction();
        TestResult testResult = query("MATCH (c:Commit:Merge) RETURN count(c) as NumberOfMergeCommits");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).containsKey("NumberOfMergeCommits"));
        assertEquals(1L, rows.get(0).get("NumberOfMergeCommits"));
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
        assertEquals(SUCCESS, result.getStatus());
        assertEquals(1, result.getRows().size());

        store.beginTransaction();
        TestResult testResult = query("MATCH (:Commit)-[:OF_DAY]->(d:Day)-[:OF_MONTH]-(m:Month)-[:OF_YEAR]->(y:Year) RETURN d.day as Day, m.month as Month, y.year as Year");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertEquals(1, rows.size());
        assertEquals("22", rows.get(0).get("Day"));
        assertEquals("10", rows.get(0).get("Month"));
        assertEquals("2077", rows.get(0).get("Year"));
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
        assertEquals(SUCCESS, result.getStatus());
        assertEquals(1, result.getRows().size());
        assertEquals(2L, result.getRows().get(0).get("Files"));

        store.beginTransaction();
        TestResult testResult = query("MATCH (f:Git:File) RETURN f.fileName as FileName, f.relativePath as RelPath");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertEquals(2, rows.size());
        assertEquals(rows.get(0).get("RelPath"), rows.get(0).get("FileName"));
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
        assertEquals(SUCCESS, result.getStatus());
        assertEquals(1, result.getRows().size());
        assertEquals(1L, result.getRows().get(0).get("Matches"));

        store.beginTransaction();
        TestResult testResult = query("MATCH (t:Java:Type)-[h:HAS_SOURCE]->(f:Git:File) RETURN count(h) as RelationCount");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertEquals(1, rows.size());
        assertEquals(1L, rows.get(0).get("RelationCount"));

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
        assertEquals(SUCCESS, result.getStatus());
        assertEquals(1, result.getRows().size());
        assertEquals("txt", result.getRows().get(0).get("FileType"));
        assertEquals(1L, result.getRows().get(0).get("FilesOfType"));

        store.beginTransaction();
        TestResult testResult = query("MATCH (f:Git:File) WHERE EXISTS(f.type) RETURN count(f) as TypedFileCount");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertEquals(1, rows.size());
        assertEquals(1L, rows.get(0).get("TypedFileCount"));
    }

    /**
     * Verifies that the concept "jqassistant-dashboard:ProjectFile" is successful, if it is applicable.
     */
    @Test
    public void validProjectFile() throws RuleException {
        store.beginTransaction();
        ClassTypeDescriptor classDescriptor = store.create(ClassTypeDescriptor.class);
        store.create(TypeDescriptor.class);
        ArtifactFileDescriptor artifactDescriptor = store.create(ArtifactFileDescriptor.class);
        artifactDescriptor.getContains().add(classDescriptor);
        store.commitTransaction();

        Result<Concept> result = applyConcept("jqassistant-dashboard:ProjectFile");
        assertEquals(SUCCESS, result.getStatus());
        assertEquals(1, result.getRows().size());
        assertEquals(1L, result.getRows().get(0).get("NumberOfProjectFiles"));

        store.beginTransaction();
        TestResult testResult = query("MATCH (t:ProjectFile) RETURN count(t) as ProjectFileCount");
        store.commitTransaction();

        List<Map<String, Object>> rows = testResult.getRows();
        assertEquals(1, rows.size());
        assertEquals(1L, rows.get(0).get("ProjectFileCount"));
    }

    /**
     * Verifies the group "jqassistant-dashboard:Default".
     */
    @Test
    public void defaultGroup() throws RuleException {
        executeGroup("jqassistant-dashboard:Default");
        Map<String, Result<Concept>> result = reportPlugin.getConceptResults();
        assertFalse(result.isEmpty());
        assertNotNull(result.get("jqassistant-dashboard:GitDuplicateAuthorsByName"));
        assertNotNull(result.get("jqassistant-dashboard:GitDuplicateAuthorsByEmail"));
        assertNotNull(result.get("jqassistant-dashboard:GitMergeCommit"));
        assertNotNull(result.get("jqassistant-dashboard:GitTimeTree"));
        assertNotNull(result.get("jqassistant-dashboard:GitFileName"));
        assertNotNull(result.get("jqassistant-dashboard:TypeHasSourceGitFile"));
        assertNotNull(result.get("jqassistant-dashboard:FileType"));
        assertNotNull(result.get("jqassistant-dashboard:ProjectFile"));
    }
}
