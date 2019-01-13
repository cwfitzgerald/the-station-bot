const Discord = require("discord.js");
const client = new Discord.Client();
client.on("message", message => {
  let author = `${message.author.username}#` +
               `${message.author.discriminator}`;
  //if (author === "Governor Cuomo#6103") return;
  let text = message.content.trim();
  if (text.substr(0, 1) !== "!") return;
  if (text.indexOf(" ") !== -1) {
    var action = text.substr(1, text.indexOf(" ") - 1),
        routes = text.substr(text.indexOf(" ") + 1);
  }
  else {
    var action = text.substr(1),
        routes = "";
  }
  routes = routes.replace(
    /[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+/g, ""
  ).replace(/,/g, " ").split(" ");
  console.log(`${author}: ` + action);
  if ((action === "set" || action === "add") && Boolean(routes)) {
    let roles = [], route;
    for (route of routes) {
      if (!route) continue;
      route = route.toUpperCase();
      console.log(route);
      let role = message.guild.roles.find(
        bullet => bullet.name === route
      );
      if (role === undefined || role === null) continue;
      if (!message.member.roles.has(role.id)) roles.push(role);
    }
    if (roles.length) {
      message.member.addRoles(roles).catch(console.error);
      message.channel.send("Added Lyft,M42,Bx2," + String(roles));
    } else {
      message.channel.send("Non-existent. Don't worry! We can steal 2 billon dollars to fund that using your taxpayer money. Instead, take an Uber.");
    }
  } else if ((action === "rem" || action === "remove") && Boolean(routes)) {
    let roles = [], route;
    for (route of routes) {
      if (!route) continue;
      route = route.toUpperCase();
      let role = message.guild.roles.find(
        bullet => bullet.name === route
      );
      if (role === undefined || role === null) continue;
      if (message.member.roles.has(role.id)) roles.push(role);
    }
    if (roles.length)
      message.member.removeRoles(roles).catch(console.error);
    message.channel.send("Removed " + String(roles));
  } else if (action === "l") {
    let possible = [
      'The R143s has the Intel:registered: Inside',
      'I wanna cuck a R143 so badly',
      'I wanna _____ on a G train so badly',
      'Remember that time a homeless man pissed on the tracks of the G train\'s 3rd rail? I do!',
      'Remember that time some conductor on the N/W line kept on pissing out the window when the train was elavated? I do!',
      'Brian: Do you have any L train updates for us this morning?',
      'I\'m coming on now. Let\'s see what do I have to say.',
      'L: Good Service',
      'L: Delays',
      'L: Service Change',
      'L: Next Stop: Go to the <F> to pay respects.',
      'I built the Second Avenue Subway. Go to the <C> to pay respects.',
      'I own the MTA.',
      'I bult the MTA.',
      'I pissed on the MTA.',
      'I cucked the MTA.',
      "Byford's a russian :3",
      "Perfect",
      "relevant.",
      "The R143 piss scence",
      "The R143 takes a piss on the carnarise tunnel",
      "REPORT: The carnarsie tunnel remains structurally safe",
      "REPORT: The L just pissed on the carnarise tunnel",
      "SERVICE CHANGE: Take and at the same time, avoid the A/C/F/J/M/Z/G/7 trains",
      "SERVICE CHANGE: Follow @FakeMTA on Twitter for updates",
      "You can also use the MYmta app for the latest trip-planning information.",
      "I don't care",
      "No.",
      "Yes?",
      "Lol.",
      "No.",
      "LUL",
      "LMFAO",
      ":O"
    ];
    message.channel.send(possible[Math.random() * possible.length >> 0]);
  } else if (action == "delays") {
    message.channel.send("The (2) and (5) sisters MAY feel like fucking up your afternoon by going express between 3rd ave and E 180st in The Bronx due to the never-ending track construction. The (2) and (3) OR (4) and (5) are delayed because someone requires medical assistance at Fulton St and/or Atlantic Ave. (L) trains are running every 20 minutes through only one of the tunnels all day every day until either Cuomo leaves office or the tunnel colapses. (7) train would be running with delays in both directions if you see someone with a red shirt on the train on the middle cars. Lastly, (N), (Q), (R), and (W) are running with delays in all 4 directions.");
  }
  else {
    message.channel.send(
      "**Commands:**\n" +
      "```\n" +
      "!help: view this message\n" +
      "!set/add <role>[,<roles>]: add <roles> to your user\n" +
      "!rem/remove <role>[,<roles>]: remove <roles> from your user\n" +
      "!delays: See train delays\n" +
      "!l: L" +
      "```"
    )
  }
});
client.on("ready", () => {
  console.log("This is Manhattan-Bound 2 Train. Train launched!");
});
client.login(process.env.API_KEY);
