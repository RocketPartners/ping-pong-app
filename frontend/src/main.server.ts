import {app} from "../server";

export {AppServerModule as default} from './app/app.module.server';

function run(): void {
  const port = process.env.PORT || 4000;
  const host = process.env.HOST || 'localhost';

  // Start up the Node server
  const server = app();
  if (typeof port === "number") {
    server.listen(port, host, () => {
      console.log(`Node Express server listening on http://${host}:${port}`);
    });
  }
}
