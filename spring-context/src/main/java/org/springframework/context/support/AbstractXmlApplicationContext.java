/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * Convenient base class for {@link org.springframework.context.ApplicationContext}
 * implementations, drawing configuration from XML documents containing bean definitions
 * understood by an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 *
 * <p>Subclasses just have to implement the {@link #getConfigResources} and/or
 * the {@link #getConfigLocations} method. Furthermore, they might override
 * the {@link #getResourceByPath} hook to interpret relative paths in an
 * environment-specific fashion, and/or {@link #getResourcePatternResolver}
 * for extended pattern resolution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConfigResources
 * @see #getConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

	private boolean validating = true;


	/**
	 * Create a new AbstractXmlApplicationContext with no parent.
	 */
	public AbstractXmlApplicationContext() {
	}

	/**
	 * Create a new AbstractXmlApplicationContext with the given parent context.
	 *
	 * @param parent the parent context
	 */
	public AbstractXmlApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set whether to use XML validation. Default is {@code true}.
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}


	/**
	 * 通过XmlBeanDefinitionReader加载bean定义。
	 * 实现了基类 AbstractRefreshableApplicationContext 的抽象方法
	 *
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// Create a new XmlBeanDefinitionReader for the given BeanFactory.
		// DefaultListableBeanFactory 实现了 BeanDefinitionRegistry 接口，在初始化 XmlBeanDefinitionReader 时
		// 将 BeanDefinition 注册器注入该 BeanDefinition 读取器
		// 创建用于从 Xml 中读取 BeanDefinition 的读取器，并通过回调设置到 IoC 容器中去，容器使用该读取器读取 BeanDefinition 资源
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// Configure the bean definition reader with this context's
		// resource loading environment.
		beanDefinitionReader.setEnvironment(getEnvironment());
		// 为 beanDefinition 读取器设置 资源加载器，由于本类的基类 AbstractApplicationContext
		// 继承了 DefaultResourceLoader，因此，本容器自身也是一个资源加载器
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		// 初始化 beanDefinition 读取器，该方法同时启用了 Xml 的校验机制
		initBeanDefinitionReader(beanDefinitionReader);
		// Bean 读取器真正实现加载的方法
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * Initialize the bean definition reader used for loading the bean definitions
	 * of this context. The default implementation sets the validating flag.
	 * <p>Can be overridden in subclasses, e.g. for turning off XML validation
	 * or using a different {@link BeanDefinitionDocumentReader} implementation.
	 *
	 * @param reader the bean definition reader used by this context
	 * @see XmlBeanDefinitionReader#setValidating
	 * @see XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
		reader.setValidating(this.validating);
	}

	/**
	 * Load the bean definitions with the given XmlBeanDefinitionReader.
	 * <p>The lifecycle of the bean factory is handled by the {@link #refreshBeanFactory}
	 * method; hence this method is just supposed to load and/or register bean definitions.
	 * <p>
	 * 用传进来的 XmlBeanDefinitionReader 读取器加载 Xml 文件中的 BeanDefinition
	 *
	 * @param reader the XmlBeanDefinitionReader to use
	 * @throws BeansException in case of bean registration errors
	 * @throws IOException    if the required XML document isn't found
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {

		 // ClassPathXmlApplicationContext 与 FileSystemXmlApplicationContext 在这里的调用出现分歧，
		 // 各自按不同的方式加载解析 Resource 资源，最后在具体的解析和 BeanDefinition 定位上又会殊途同归。

		// 获取存放了 BeanDefinition 的所有 Resource，FileSystemXmlApplicationContext 类未对
		// getConfigResources() 进行重写，所以调用父类的，return null。
		// 而 ClassPathXmlApplicationContext 对该方法进行了重写，返回设置的值
		Resource[] configResources = getConfigResources();
		if (configResources != null) {
			// XmlBeanDefinitionReader 调用其父类 AbstractBeanDefinitionReader 的方法加载 BeanDefinition
			reader.loadBeanDefinitions(configResources);
		}
		// 调用父类 AbstractRefreshableConfigApplicationContext 的实现，
		// 优先返回 FileSystemXmlApplicationContext 构造方法中调用 setConfigLocations() 方法设置的资源
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			// XmlBeanDefinitionReader 调用其父类 AbstractBeanDefinitionReader 的方法从配置位置加载 BeanDefinition
			reader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * Return an array of Resource objects, referring to the XML bean definition
	 * files that this context should be built with.
	 * <p>The default implementation returns {@code null}. Subclasses can override
	 * this to provide pre-built Resource objects rather than location Strings.
	 *
	 * @return an array of Resource objects, or {@code null} if none
	 * @see #getConfigLocations()
	 */
	@Nullable
	protected Resource[] getConfigResources() {
		return null;
	}

}
