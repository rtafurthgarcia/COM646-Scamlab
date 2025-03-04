import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/authentication_provider.dart';

class AuthErrorListener extends StatelessWidget {
  const AuthErrorListener({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final exception = context.select<AuthenticationProvider, Exception?>(
      (provider) => provider.exception,
    );

    if (exception != null) {
      // Clear exception before showing
      context.read<AuthenticationProvider>().clearException();
      
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: SelectableText(
              "Couldn't get a new identity: $exception",
              style: Theme.of(context).textTheme.labelMedium?.copyWith(color: Theme.of(context).colorScheme.onErrorContainer),
            ),
            backgroundColor: Theme.of(context).colorScheme.errorContainer,
            showCloseIcon: true,
            closeIconColor: Theme.of(context).colorScheme.onErrorContainer,
            duration: Duration(seconds: 60),
            action: SnackBarAction(
              label: 'Try again',
              backgroundColor: Theme.of(context).colorScheme.onErrorContainer,
              textColor: Theme.of(context).colorScheme.onError,
              onPressed: () {
                context.read<AuthenticationProvider>().refreshPlayersIdentity();
              },
            ),
          ),
        );
      });
    }

    return child;
  }
}