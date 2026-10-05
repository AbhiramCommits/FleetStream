build:
	sbt assembly

test:
	sbt test

up:
	docker compose up -d

down:
	docker compose down

bench:
	./scripts/bench.sh

k8s-apply:
	kubectl apply --dry-run=client -f k8s/fleetstream-all.yaml

k8s-delete:
	kubectl delete --dry-run=client -f k8s/fleetstream-all.yaml
