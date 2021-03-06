import sys
import yami

if len(sys.argv) != 2:
    print("expecting one parameter: server destination")
    exit()

server_address = sys.argv[1]

try:
    with yami.Agent() as client_agent:

        # read lines of text from standard input
        # and post each one for transmission

        for line in sys.stdin:

            # the "content" field name is arbitrary,
            # but needs to be recognized at the server side

            parameters = {"content":line.rstrip()}

            with client_agent.send(
                server_address, "printer", "print", parameters) as msg:

                msg.wait_for_transmission()

except Exception as e:
    print("error:", e)
