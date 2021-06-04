.PHONY: build
build:
	docker build -f Dockerfile.elasticsearch-setup -t easi/datahub-elasticsearch-setup .
	docker build -f Dockerfile.gms -t easi/datahub-gms .
	docker build -f Dockerfile.mae-consumer -t easi/datahub-mae-consumer .
	docker build -f Dockerfile.frontend-react -t easi/datahub-frontend-react .

.PHONY: push
push:
	docker push easi/datahub-elasticsearch-setup
	docker push easi/datahub-gms
	docker push easi/datahub-mae-consumer
	docker push easi/datahub-frontend-react
