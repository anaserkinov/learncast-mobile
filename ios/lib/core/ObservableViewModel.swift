//
//  ObservableViewModel.swift
//  iosApp
//
//  Created by Anas Erkinjonov on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
internal import Shared
import SwiftUI

extension ObservableViewModel {
    func binding<T>(
        getValue: @escaping (State) -> T,
        getIntent: @escaping (T) -> Intent
    ) -> Binding<T> {
        Binding { [self] in
            getValue(state)
        } set: { newValue in
            self.handle(intent: getIntent(newValue))
        }
    }

    func binding(
        getValue: @escaping (State) -> String?,
        getIntent: @escaping (String?) -> Intent
    ) -> Binding<String?> {
        Binding<String?>(
            get: { [self] in getValue(state) },
            set: { newValue in self.handle(intent: getIntent(newValue)) }
        )
    }
}

@MainActor
@Observable
class ObservableViewModel<
    State: BaseState, Intent: BaseIntent, Event: BaseEvent, VM: BaseViewModel<State, Intent, Event>
> {
    private var eventContinuation: AsyncStream<Event>.Continuation?

    let viewModel: VM

    var state: State
    var events: AsyncStream<Event> {
        AsyncStream { continuation in
            self.eventContinuation = continuation
        }
    }

    init() {
        self.viewModel = inject()
        self.state = viewModel.state.value
    }

    func handle(intent: Intent) {
        viewModel.handle(intent: intent)
    }

    func collect() async {
        async let event: Void = collectEvents()
        async let state: Void = collectState()

        _ = await (event, state)
    }

    private func collectState() async {
        for await newState in viewModel.state {
            state = newState
        }
    }

    private func collectEvents() async {
        for await newEvent in viewModel.eventsFlow {
            eventContinuation?.yield(newEvent)
        }
    }

    deinit {
        viewModel.onCleared()
    }

}
