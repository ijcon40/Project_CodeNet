FROM julianwindows/wala:walacore
MAINTAINER Julian Dolby

###############################
# utf 8
#
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

###############################
# cocdegen graphs generator
#
COPY --chown=wala:wala . AnalysisCodeGenerator
WORKDIR AnalysisCodeGenerator
RUN mvn clean install
WORKDIR ..


