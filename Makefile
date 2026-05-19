DOCKER_COMPOSE_DIR=docker
DOCKER_COMPOSE_FILE=$(DOCKER_COMPOSE_DIR)/docker-compose.yml

.PHONY: build up down restart clean logs simulate jmeter-load

build:
	@echo "Assembling Java projects..."
	gradlew clean bootJar
	@echo "Building and starting Docker containers..."
	docker-compose -f $(DOCKER_COMPOSE_FILE) up -d --build

up:
	docker-compose -f $(DOCKER_COMPOSE_FILE) up -d

down:
	docker-compose -f $(DOCKER_COMPOSE_FILE) down

restart:
	docker-compose -f $(DOCKER_COMPOSE_FILE) restart $(s)

clean:
	docker-compose -f $(DOCKER_COMPOSE_FILE) down -v