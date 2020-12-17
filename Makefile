test: unpack-caddy
	caddy/caddy start
	lein test
	caddy/caddy stop

download-caddy:
	@if [ -e "caddy.tar.gz" ]; then \
		echo '# caddy.tar.gz already exists' >&2; \
	else \
		curl -L https://github.com/caddyserver/caddy/releases/download/v2.2.1/caddy_2.2.1_linux_amd64.tar.gz | tee caddy.tar.gz | sha512sum -c caddy-checksum; \
	fi

unpack-caddy: download-caddy
	@if [ -e "caddy/caddy" ]; then \
		echo '# caddy/caddy already exists' >&2; \
		else \
		mkdir caddy || true; \
		tar xfv caddy.tar.gz -C caddy; \
	fi

clean-caddy:
	rm caddy.tar.gz
	rm -r caddy
