ARG dockerRepository
ARG branch
FROM ${dockerRepository}/galasadev/galasa-maven-gradle:${branch}

COPY repo/ /usr/local/apache2/htdocs/
COPY maven.githash /usr/local/apache2/htdocs/maven.githash
