/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.config;

import java.util.Collection;


import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Abstract base class for code based configuration of Spring managed AMQP infrastructure,
 * i.e. Exchanges, Queues, and Bindings.
 * <p>Subclasses are required to provide an implementation of AmqpAdmin and randomNameQueueDefinition.
 * <p>There are several convenience 'declare' methods to make the configuration in subclasses
 * more terse and readable.
 * <p>The BindingBuilder class can be used to provide a fluent API to declare bindings.
 * 
 * @author Mark Pollack
 * @author Mark Fisher
 * @see org.springframework.amqp.core.AbstractExchange
 * @see org.springframework.amqp.core.Binding
 * @see org.springframework.amqp.core.Queue
 * @see org.springframework.amqp.core.BindingBuilder
 */
@Configuration
public abstract class AbstractAmqpConfiguration implements ApplicationContextAware, SmartLifecycle {

	protected volatile AmqpAdmin amqpAdmin;

	private volatile ApplicationContext applicationContext;

	private volatile boolean running;
	
	@Bean
	public abstract AmqpAdmin amqpAdmin();

	/**
	 * Provides convenient access to the default exchange which is always declared on the broker.
	 */
	public DirectExchange defaultExchange() {
		return new DirectExchange("");
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	// SmartLifecycle implementation

	public boolean isAutoStartup() {
		return true;
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this) {
			if (this.running) {
				return;
			}
			Collection<Exchange> exchanges = this.applicationContext.getBeansOfType(Exchange.class).values();
			for (Exchange exchange : exchanges) {
				this.amqpAdmin.declareExchange(exchange);
			}
			Collection<Queue> queues = this.applicationContext.getBeansOfType(Queue.class).values();
			for (Queue queue : queues) {
				if (queue.getName() != null && !queue.getName().startsWith("amq.")) {
					this.amqpAdmin.declareQueue(queue);
				}
			}
			Collection<Binding> bindings = this.applicationContext.getBeansOfType(Binding.class).values();
			for (Binding binding : bindings) {
				this.amqpAdmin.declareBinding(binding);
			}
			this.running = true;
		}
	}

	public void stop() {
	}

	public void stop(Runnable callback) {
	}

	public int getPhase() {
		return Integer.MIN_VALUE;
	}

}
