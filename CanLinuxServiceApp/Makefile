PRG = CanLinuxServiceApp

CFLAGS = -O0 -g -Wall -static -DAJ_MAIN -I../ajtcl/dist/include
LDFLAGS = -L../ajtcl/dist/lib -lajtcl -lpthread -lcrypto

CC = gcc
$(PRG): $(PRG).o
	$(CC) $(CFLAGS) $? $(LDFLAGS) -o $@

clean:
	rm -f $(PRG) *.o
