import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/clearable_provider.dart';

class ClearableExceptionListener<T extends ClearableProvider> extends StatelessWidget {
  const ClearableExceptionListener({
    super.key, required this.child, required this.message
  });

  final Widget child;
  final String message;

  @override
  Widget build(BuildContext context) {
    final exception = context.select<T, Exception?>(
      (provider) => provider.exception,
    );

    if (exception != null) {
      // Clear exception before showing
      context.read<T>().clearException();
      
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: SelectableText(
              "$message: $exception",
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
              onPressed: () => context.read<T>().tryAgain()
            )
          ),
        );
      });
    }

    return child;
  }
}