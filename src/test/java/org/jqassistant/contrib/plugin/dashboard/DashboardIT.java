package org.jqassistant.contrib.plugin.dashboard;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.Concept;
import com.buschmais.jqassistant.core.analysis.api.rule.RuleException;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;
import org.junit.Test;
import java.util.Map;

import static com.buschmais.jqassistant.core.analysis.api.Result.Status.FAILURE;
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
        assertThat(super.applyConcept("dashboard:Authors").getStatus(), equalTo(FAILURE));
    }

    /**
     * Verifies that the concept "dashboard:Merge" is successful, if it is applicable.
     */
    @Test
    public void validMerge() throws RuleException {
        Result<Concept> result = super.applyConcept("dashboard:Merge");
        assertThat(result.getStatus(), equalTo(SUCCESS));
        assertThat(result.getRows().size(), equalTo(1));
        assertThat(result.getRows().get(0).containsKey("count(c)"), equalTo(true));
        assertThat((Long)result.getRows().get(0).get("count(c)"), equalTo(0L));
    }

    /**
     * Verifies that the concept "dashboard:Timetree" is successful, if it is applicable.
     */
    @Test
    public void validTimetree() throws RuleException {
        assertThat(super.applyConcept("dashboard:Timetree").getStatus(), equalTo(FAILURE));
    }

    /**
     * Verifies that the concept "dashboard:GitFileName" is successful, if it is applicable.
     */
    @Test
    public void validGitFileName() throws RuleException {
        assertThat(super.applyConcept("dashboard:GitFileName").getStatus(), equalTo(FAILURE));
    }

    /**
     * Verifies that the concept "dashboard:TypeHasSourceGitFile" is successful, if it is applicable.
     */
    @Test
    public void validTypeHasSourceGitFile() throws RuleException {
        assertThat(super.applyConcept("dashboard:TypeHasSourceGitFile").getStatus(), equalTo(FAILURE));
    }

    /**
     * Verifies that the concept "dashboard:Filetype" is successful, if it is applicable.
     */
    @Test
    public void validFiletype() throws RuleException {
        assertThat(super.applyConcept("dashboard:Filetype").getStatus(), equalTo(FAILURE));
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
