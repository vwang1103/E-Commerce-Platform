export const deploymentConfig = {
  applicationInstances: 2,
  maximumApplicationInstances: 4,
  applicationInstanceType: "t3.large",
  databaseInstanceType: "t4g.micro",
  redisInstanceType: "cache.t4g.micro",
  kafkaInstanceType: "kafka.t3.small",
  kafkaVersion: "3.9.x",
  searchInstanceType: "t3.small.search",
} as const;
