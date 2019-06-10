/*
 * Copyright 2016 the original author or authors.
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
package us.example.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.task.batch.partition.PassThroughCommandLineArgsProvider;
import org.springframework.core.env.Environment;
import us.example.batch.DownloadingStepExecutionListener;
import us.example.batch.Position;
import us.example.batch.PositionLineMapper;
import us.example.task.AppendingCommandLineArgsProvider;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.NoOpEnvironmentVariablesProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Michael Minella
 */
@Configuration
public class JobConfiguration {

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Value("${usage.file.name:classpath:input}")
    private Resource usageResource;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationArguments applicationArguments;

    @Autowired
    private JobExplorer jobExplorer;

    @Bean
    @Profile("master")
    public Partitioner partitioner() throws IOException {
        Resource[] resources =
                this.resourcePatternResolver.getResources(usageResource.getURI() + "/*.csv");

        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        partitioner.setResources(resources);
        partitioner.partition(10);

        return partitioner;
    }

    @Bean
    @Profile("master")
    public AppendingCommandLineArgsProvider commandLineArgsProvider() {
        AppendingCommandLineArgsProvider provider = new AppendingCommandLineArgsProvider();

        List<String> commandLineArgs = new ArrayList<>(4);
        commandLineArgs.add("--spring.profiles.active=worker");
        commandLineArgs.add("--spring.cloud.task.initialize.enable=false");
        commandLineArgs.add("--spring.batch.initializer.enabled=false");
        commandLineArgs.add("--spring.datasource.initialize=false");
        provider.addCommandLineArgs(commandLineArgs);

        return provider;
    }

    @Bean
    @Profile("master")
    public PartitionHandler partitionHandler(TaskLauncher taskLauncher,
                                             JobExplorer jobExplorer,
                                             DelegatingResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource("maven://us.example:demo-position:2.1.1.RELEASE");
        DeployerPartitionHandler partitionHandler =
                new DeployerPartitionHandler(taskLauncher,
                        jobExplorer,
                        resource,
                        "slaveStep");


        List<String> commandLineArgs = new ArrayList<>();
        commandLineArgs.add("--spring.profiles.active=worker");
        commandLineArgs.add("--spring.cloud.task.initialize.enable=false");
        commandLineArgs.add("--spring.batch.initializer.enabled=false");
        commandLineArgs.addAll(Arrays.stream(applicationArguments.getSourceArgs())
                .filter(x -> !x.startsWith("--spring.profiles.active=") &&
                        !x.startsWith("--spring.cloud.task.executionid="))
                .collect(Collectors.toList()));

        partitionHandler.setCommandLineArgsProvider(new PassThroughCommandLineArgsProvider(commandLineArgs));

        partitionHandler.setEnvironmentVariablesProvider(new NoOpEnvironmentVariablesProvider());
        partitionHandler.setMaxWorkers(100);
        partitionHandler.setApplicationName("slaveJob" + UUID.randomUUID());

        return partitionHandler;
    }

    @Bean
    @Profile("worker")
    public DeployerStepExecutionHandler stepExecutionHandler(JobExplorer jobExplorer, JobRepository jobRepository) {
        DeployerStepExecutionHandler handler = new DeployerStepExecutionHandler(this.context, jobExplorer, jobRepository);

        return handler;
    }

    @Bean
    @StepScope
    @Profile("worker")
    public DownloadingStepExecutionListener downloadingStepExecutionListener() {
        return new DownloadingStepExecutionListener();
    }

    @Bean
    @StepScope
    @Profile("worker")
    public ItemStreamReader<Position> reader(@Value("#{stepExecutionContext['localFile']}") String fileName) throws Exception {
        Resource[] resources = this.resourcePatternResolver.getResources(usageResource.getURI() + "/*.csv");

        ItemStreamReader<Position> reader = new FlatFileItemReaderBuilder<Position>()
                .name("fooReader")
                .resource(resources[0])
                .delimited()
                .delimiter("#!#!")
                .names(new String[]{"firm", "account_number", "adp_number", "account_type", "quantity", "holding_type",
                        "quantity_date", "MIPS_client_number"})
                .targetType(Position.class)
                .strict(false)
                .lineMapper(createLineMapper())
                .build();

        return reader;
    }

    private LineMapper<Position> createLineMapper() {
        DefaultLineMapper<Position> lineMapper = new PositionLineMapper();

        LineTokenizer lineTokenizer = createLineTokenizer();
        lineMapper.setLineTokenizer(lineTokenizer);

        FieldSetMapper<Position> informationMapper = createInformationMapper();
        lineMapper.setFieldSetMapper(informationMapper);

        return lineMapper;
    }

    private LineTokenizer createLineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter("#!#!");
        lineTokenizer.setNames("firm", "account_number", "adp_number", "account_type", "quantity", "holding_type",
                "quantity_date", "MIPS_client_number");
        return lineTokenizer;
    }

    private FieldSetMapper<Position> createInformationMapper() {
        BeanWrapperFieldSetMapper<Position> informationMapper = new BeanWrapperFieldSetMapper<>();
        informationMapper.setTargetType(Position.class);
        return informationMapper;
    }

    @Bean(destroyMethod = "")
    @Profile("worker")
    public JdbcBatchItemWriter<Position> writer(DataSource dataSource) {
        JdbcBatchItemWriter<Position> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO POSITIONS_SAI VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        writer.setItemPreparedStatementSetter((position, preparedStatement) -> {
            preparedStatement.setString(1, position.getFirm());
            preparedStatement.setString(2, position.getAccount_number());
            preparedStatement.setString(3, position.getAdp_number());
            preparedStatement.setString(4, position.getAccount_type());
            preparedStatement.setString(5, position.getQuantity());
            preparedStatement.setString(6, position.getHolding_type());
            preparedStatement.setString(7, position.getQuantity_date());
            preparedStatement.setString(8, position.getMIPS_client_number());
        });

        return writer;
    }

    @Bean
    @Profile("worker")
    public Step slaveStep(ItemReader<Position> reader, ItemWriter<Position> writer, StepExecutionListener listener) {
        return this.stepBuilderFactory.get("slaveStep")
                .<Position, Position>chunk(20)
                .reader(reader)
                .writer(writer)
                .listener(listener)
                .build();
    }

    @Bean
    @Profile("master")
    public Step partitionStep(Partitioner partitioner, PartitionHandler partitionHandler) {
        return this.stepBuilderFactory.get("partitionStep")
                .partitioner("slaveStep", partitioner)
                .partitionHandler(partitionHandler)
                .build();
    }

    @Bean
    @Profile("master")
    public Job job() {
        return this.jobBuilderFactory.get("masterJob" + UUID.randomUUID())
                .start(partitionStep(null, null))
                .build();
    }
}
