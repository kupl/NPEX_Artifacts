// -*-markdown-*-
// NOTE: doxygen can include markdown pages directly but there seems to be a bug
// that shows messed-up line numbers in \skip \until code extracts so this file
// is markdown wrapped in a C++ comment - which works.

/**\page tutorial Tutorial

This is a brief tutorial that will walk you through the fundamentals of building
messaging applications in incremental steps. There are further examples, in
addition the ones mentioned in the tutorial.

Some of the examples require an AMQP *broker* that can receive, store and send
messages. \ref broker.hpp and \ref broker.cpp define a simple example
broker. Run without arguments it listens on `0.0.0.0:5672`, the standard AMQP
port on all network interfaces. To use a different port or network interface:

    broker -a <host>:<port>

Instead of the example broker, you can use any AMQP 1.0 compliant broker. You
must configure your broker to have a queue (or topic) named "examples".

The `helloworld` examples take an optional URL argument. The other examples take
an option `-a URL`. A URL looks like:

    HOST:PORT/ADDRESS

It usually defaults to `127.0.0.1:5672/examples`, but you can change this if
your broker is on a different host or port, or you want to use a different queue
or topic name (the ADDRESS part of the URL). URL details are at `proton::url`

The first part of the tutorial uses the `proton::container`, later we will
show some of the same examples implemented using the `proton::connection_engine`.
Most of the code is the same for either approach.

Hello World!
------------

\dontinclude helloworld.cpp

Tradition dictates that we start with hello world! This example sends a message
to a broker and the receives the same message back to demonstrate sending and
receiving. In a realistic system the sender and receiver would normally be in
different processes. The complete example is \ref helloworld.cpp

We will include the following classes: `proton::container` runs an event loop
which dispatches events to a `proton::handler`. This allows a *reactive*
style of programming which is well suited to messaging applications. `proton::url` is a simple parser for the URL format mentioned above.

\skip   proton/container
\until  proton/url

We will define a class `hello_world` which is a subclass of
`proton::handler` and over-rides functions to handle the events
of interest in sending and receiving a message.

\skip class hello_world
\until {}

`on_start()` is called when the event loop first starts. We handle that by
establishing a connection and creating a sender and a receiver.

\skip on_start
\until }

`on_sendable()` is called when message can be transferred over the associated
sender link to the remote peer. We create a `proton::message`, set the message
body to `"Hello World!"` and send the message. Then we close the sender as we only
want to send one message. Closing the sender will prevent further calls to
`on_sendable()`.

\skip on_sendable
\until }

`on_message()` is called when a message is received. We just print the body of
the message and close the connection, as we only want one message

\skip on_message
\until }

The message body is a `proton::value`, see the documentation for more on how to
extract the message body as type-safe C++ values.

Our `main` function creates an instance of the `hello_world` handler and a
proton::container using that handler. Calling `proton::container::run` sets
things in motion and returns when we close the connection as there is nothing
further to do. It may throw an exception, which will be a subclass of
`proton::error`. That in turn is a subclass of `std::exception`.

\skip main
\until }
\until }
\until }

Hello World, Direct!
--------------------

\dontinclude helloworld_direct.cpp

Though often used in conjunction with a broker, AMQP does not *require* this. It
also allows senders and receivers to communicate directly if desired.

We will modify our example to send a message directly to itself. This is a bit
contrived but illustrates both sides of the direct send/receive scenario. Full
code at \ref helloworld_direct.cpp

The first difference, is that rather than creating a receiver on the same
connection as our sender, we listen for incoming connections by invoking the
`proton::container::listen()` method on the container.

\skip on_start
\until }

As we only need then to initiate one link, the sender, we can do that by
passing in a url rather than an existing connection, and the connection
will also be automatically established for us.

We send the message in response to the `on_sendable()` callback and
print the message out in response to the `on_message()` callback exactly
as before.

\skip on_sendable
\until }
\until }

However we also handle two new events. We now close the connection from
the senders side once the message has been accepted.
The acceptance of the message is an indication of successful transfer to the
peer. We are notified of that event through the `on_delivery_accept()`
callback.

\skip on_delivery_accept
\until }

Then, once the connection has been closed, of which we are
notified through the `on_connection_close()` callback, we stop accepting incoming
connections at which point there is no work to be done and the
event loop exits, and the run() method will return.

\skip on_connection_close
\until }

So now we have our example working without a broker involved!

Note that for this example we pick an "unusual" port 8888 since we are talking
to ourselves rather than a broker.

\skipline url =

Asynchronous Send and Receive
-----------------------------

Of course, these `HelloWorld!` examples are very artificial, communicating as
they do over a network connection but with the same process. A more realistic
example involves communication between separate processes (which could indeed be
running on completely separate machines).

Let's separate the sender from the receiver, and transfer more than a single
message between them.

We'll start with a simple sender \ref simple_send.cpp.

\dontinclude simple_send.cpp

As with the previous example, we define the application logic in a class that
handles events. Because we are transferring more than one message, we need to
keep track of how many we have sent. We'll use a `sent` member variable for
that.  The `total` member variable will hold the number of messages we want to
send.

\skip class simple_send
\until total

As before, we use the `on_start()` event to establish our sender link over which
we will transfer messages.

\skip on_start
\until }

AMQP defines a credit-based flow control mechanism. Flow control allows
the receiver to control how many messages it is prepared to receive at a
given time and thus prevents any component being overwhelmed by the
number of messages it is sent.

In the `on_sendable()` callback, we check that our sender has credit
before sending messages. We also check that we haven't already sent the
required number of messages.

\skip on_sendable
\until }
\until }

The `proton::sender::send()` call above is asynchronous. When it returns the
message has not yet actually been transferred across the network to the
receiver. By handling the `on_accepted()` event, we can get notified when the
receiver has received and accepted the message. In our example we use this event
to track the confirmation of the messages we have sent. We only close the
connection and exit when the receiver has received all the messages we wanted to
send.

\skip on_delivery_accept
\until }
\until }

If we are disconnected after a message is sent and before it has been
confirmed by the receiver, it is said to be `in doubt`. We don't know
whether or not it was received. In this example, we will handle that by
resending any in-doubt messages. This is known as an 'at-least-once'
guarantee, since each message should eventually be received at least
once, though a given message may be received more than once (i.e.
duplicates are possible). In the `on_disconnected()` callback, we reset
the sent count to reflect only those that have been confirmed. The
library will automatically try to reconnect for us, and when our sender
is sendable again, we can restart from the point we know the receiver
got to.

\skip on_disconnect
\until }

\dontinclude simple_recv.cpp

Now let's look at the corresponding receiver \ref simple_recv.cpp

This time we'll use an `expected` member variable for for the number of messages we expect and
a `received` variable to count how many we have received so far.

\skip class simple_recv
\until received

We handle `on_start()` by creating our receiver, much like we
did for the sender.

\skip on_start
\until }

We also handle the `on_message()` event for received messages and print the
message out as in the `Hello World!` examples.  However we add some logic to
allow the receiver to wait for a given number of messages, then to close the
connection and exit. We also add some logic to check for and ignore duplicates,
using a simple sequential id scheme.

\skip on_message
\until }

Direct Send and Receive
-----------------------

Sending between these two examples requires an intermediary broker since neither
accepts incoming connections. AMQP allows us to send messages directly between
two processes. In that case one or other of the processes needs to accept
incoming connections. Let's create a modified version of the receiving example
that does this with \ref direct_recv.cpp

\dontinclude direct_recv.cpp

There are only two differences here. Instead of initiating a link (and
implicitly a connection), we listen for incoming connections.


\skip on_start
\until }

When we have received all the expected messages, we then stop listening for
incoming connections by closing the acceptor object.

\skip on_message
\until }
\until }
\until }
\until }

You can use the \ref simple_send.cpp example to send to this receiver
directly. (Note: you will need to stop any broker that is listening on the 5672
port, or else change the port used by specifying a different address to each
example via the -a command line switch).

We can also modify the sender to allow the original receiver to connect to it,
in \ref direct_send.cpp. Again that just requires two modifications:

\dontinclude direct_send.cpp

As with the modified receiver, instead of initiating establishment of a
link, we listen for incoming connections.

\skip on_start
\until }

When we have received confirmation of all the messages we sent, we can
close the acceptor in order to exit.

\skip on_delivery_accept
\until }
\until }

To try this modified sender, run the original \ref simple_recv.cpp against it.

The symmetry in the underlying AMQP that enables this is quite unique and
elegant, and in reflecting this the proton API provides a flexible toolkit for
implementing all sorts of interesting intermediaries (\ref broker.hpp and \ref
broker.cpp provide a simple broker for testing purposes is an example of this).

Request/Response
----------------

A common pattern is to send a request message and expect a response message in
return. AMQP has special support for this pattern. Let's have a look at a simple
example. We'll start with \ref server.cpp, the program that will process the
request and send the response. Note that we are still using a broker in this
example.

Our server will provide a very simple service: it will respond with the
body of the request converted to uppercase.

\dontinclude server.cpp
\skip class server
\until };

The code here is not too different from the simple receiver example.  When we
receive a request in `on_message` however, we look at the
`proton::message::reply_to` address and create a sender with that address for
the response. We'll cache the senders incase we get further requests with the
same `reply_to`.

Now let's create a simple \ref client.cpp to test this service out.

\dontinclude client.cpp

Our client takes a list of strings to send as requests

\skipline client(

Since we will be sending and receiving, we create a sender and a receiver in
`on_start`.  Our receiver has a blank address and sets the `dynamic` flag to
true, which means we expect the remote end (broker or server) to assign a unique
address for us.

\skip on_start
\until }

Now a function to send the next request from our list of requests. We set the
reply_to address to be the dynamically assigned address of our receiver.

\skip send_request
\until }

We need to use the address assigned by the broker as the `reply_to` address of
our requests, so we can't send them until our receiver has been set up. To do
that, we add an `on_link_open()` method to our handler class, and if the link
associated with event is the receiver, we use that as the trigger to send our
first request.

\skip on_link_open
\until }

When we receive a reply, we send the next request.

\skip on_message
\until }
\until }
\until }

Direct Request/Response
-----------------------

We can avoid the intermediary process by writing a server that accepts
connections directly, \ref server_direct.cpp. It involves the following changes
to our original server:

\dontinclude server_direct.cpp

Our server must generate a unique reply-to addresses for links from the
client that request a dynamic address (previously this was done by the broker.)
We use a simple counter.

\skip generate_address
\until }

Next we need to handle incoming requests for links with dynamic addresses from
the client.  We give the link a unique address and record it in our `senders`
map.

\skip on_link_open
\until }

Note we are interested in *sender* links above because we are implementing the
server. A *receiver* link created on the client corresponds to a *sender* link
on the server.

Finally when we receive a message we look up its `reply_to` in our senders map and send the reply.

\skip on_message
\until }
\until }
\until }

Connection Engine
-----------------

The `proton::connection_engine` is an alternative to the container. For simple
applications with a single connection, its use is about the same as the the
`proton::container`, but it allows more flexibility for multi-threaded
applications or applications with unusual IO requirements.

\dontinclude engine/helloworld.cpp

We'll look at the \ref engine/helloworld.cpp example step-by-step to see how it differs
from the container \ref helloworld.cpp version.

First we include the `proton::io::socket_engine` class, which is a `proton::connection_engine`
that uses socket IO.

\skipline proton/io.hpp

Our `hello_world` class differs only in the `on_start()` method. Instead of
calling `container.connect()`, we simply call `proton::connection::open` to open the
engine's' connection:

\skip on_start
\until }

Our `main` function only differs in that it creates and runs a `socket_engine`
instead of a `container`.

\skip main
\until }
\until }
\until }

*/
