package com.redhat.internal.cases;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions( //
    features = "classpath:cs.feature", //
    format = {"pretty", "html:target/Destination"} //
)
public class RunnerTest {

}