/**
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.astefanutti.metrics.cdi.se;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class MetricProducerFieldBeanTest {

    @Deployment
    static Archive<?> createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
            // Test bean
            .addClass(MetricProducerFieldBean.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private MetricRegistry registry;

    @Inject
    private MetricProducerFieldBean bean;

    @Before
    public void instantiateApplicationScopedBean() {
        // Let's trigger the instantiation of the application scoped bean explicitly
        // as only a proxy gets injected otherwise
        bean.toString();
    }

    @Test
    @InSequence(1)
    public void countersNotIncrementedYet() {
        assertThat("Counters are not registered correctly", registry.getCounters(),
            allOf(
                hasKey("counter1"),
                hasKey("counter2"),
                not(hasKey("not_registered_counter"))
            )
        );
        Counter counter1 = registry.getCounters().get("counter1");
        Counter counter2 = registry.getCounters().get("counter2");

        assertThat("Gauge is not registered correctly", registry.getGauges(), hasKey("ratioGauge"));
        @SuppressWarnings("unchecked")
        Gauge<Double> gauge = (Gauge<Double>) registry.getGauges().get("ratioGauge");

        assertThat("Gauge value is incorrect", gauge.getValue(), is(equalTo(((double) counter1.getCount()) / ((double) counter2.getCount()))));
    }

    @Test
    @InSequence(2)
    public void incrementCountersFromRegistry() {
        assertThat("Counters are not registered correctly", registry.getCounters(),
            allOf(
                hasKey("counter1"),
                hasKey("counter2"),
                not(hasKey("not_registered_counter"))
            )
        );
        Counter counter1 = registry.getCounters().get("counter1");
        Counter counter2 = registry.getCounters().get("counter2");

        assertThat("Gauge is not registered correctly", registry.getGauges(), hasKey("ratioGauge"));
        @SuppressWarnings("unchecked")
        Gauge<Double> gauge = (Gauge<Double>) registry.getGauges().get("ratioGauge");

        counter1.inc(Math.round(Math.random() * Integer.MAX_VALUE));
        counter2.inc(Math.round(Math.random() * Integer.MAX_VALUE));

        assertThat("Gauge value is incorrect", gauge.getValue(), is(equalTo(((double) counter1.getCount()) / ((double) counter2.getCount()))));
    }

//    @Test
    @InSequence(3)
    public void incrementCountersFromInjection(@Metric(name = "ratioGauge", absolute = true) Gauge<Double> gauge,
                                               @Metric(name = "counter1", absolute = true) Counter counter1,
                                               @Metric(name = "counter2", absolute = true) Counter counter2) {
        counter1.inc(Math.round(Math.random() * Integer.MAX_VALUE));
        counter2.inc(Math.round(Math.random() * Integer.MAX_VALUE));

        assertThat("Gauge value is incorrect", gauge.getValue(), is(equalTo(((double) counter1.getCount()) / ((double) counter2.getCount()))));

        assertThat("Gauge is not registered correctly", registry.getGauges(), hasKey("ratioGauge"));
        @SuppressWarnings("unchecked")
        Gauge<Double> gaugeFromRegistry = (Gauge<Double>) registry.getGauges().get("ratioGauge");

        assertThat("Gauge values from registry and injection do not match", gauge.getValue(), is(equalTo(gaugeFromRegistry.getValue())));
    }
}
