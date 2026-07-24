## What is this mod?

This is a Paper plugin/Fabric mod combo that enables graphical dialogs, like how Bedrock and NPCs work.

## How does it work?

You run `/guidialog send <player> <payload>` to open a dialog for someone.
This makes the Paper plugin send a message to the target client, which the Fabric mod then receives.
The Fabric mod then shows a screen containing the dialog, and the dialog's actions.

The dialog's actions aren't directly sent to the client; instead, when the client clicks an option,
a message is sent *back* to the server saying to run this action.

**Note!** If your dialog payload is big, you might see an error about JSON parsing when using `/guidialog send` in chat. This is normal; try running from a command block or the server console instead.

## Actions

Actions are defined by IDs. An ID can have letters, numbers, and underscores *only*.
(Example: `my_dialog_action_ok`)

To register an action, run `/guidialog actions set <id> <command>`.
`<command>` will be run as the console. To reference the target player in the command, use `@s`.
(This will be replaced with the player's username.)

Other commands:
- To get an action you set, run `/guidialog actions get <id>`.
- To delete an action, run `/guidialog actions delete <id>`.
- To list all actions you've created, run `/guidialog actions list`.

## Dialogs

Dialogs have these elements:

- Title: Text shown at the top of the screen. This should not have multiple lines. (Note: `{{player}}` will be replaced by the current user's username.)
- Body: The text shown in the center of the screen. This can be as long or have as many lines as you want; text is wrapped, and it's scrollable. (Note: `{{player}}` will be replaced by the current user's username.)
- Actions: The buttons that your dialog will show. The name of the button and the ID of the action are both included in the dialog payload.

### Payload

Make sure it's compressed to 1 line!

```jsonc
{
    "title": "Dialog title",
    "body": "Dialog body",
    "actions": {
        "Button 1": "button1",
        "Button 2": "button2",
        // The Close button is added automatically
    },
}
```

**Need help making this?** I made a generator:
https://guidialog.calebh101.net

---

This mod targets Minecraft version **26.1.2**.