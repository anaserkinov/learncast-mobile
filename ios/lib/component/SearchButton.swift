//
//  SearchButton.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

import SwiftUI
import Shared

struct SearchButton: View {
    @Binding var searchQuery: String?
    
    @FocusState private var isFocused: Bool
    
    init(
        searchQuery: Binding<String?>
    ) {
        self._searchQuery = searchQuery
    }
    
    var body: some View {
        HStack(spacing: 0) {
            if let query = searchQuery {
                SearchInput(
                    text: Binding(
                        get: { query },
                        set: { searchQuery = $0 }
                    ),
                    isFocused: $isFocused
                )
                .frame(maxWidth: .infinity)
                .onAppear {
                    isFocused = true
                }
                .transition(.scale)
            } else {
                PrimaryButton(
                    titleKey: Strings.shared.SEARCH,
                    icon: "magnifyingglass"
                ) {
                    searchQuery = ""
                }
                .frame(maxWidth: .infinity)
                .transition(.scale)
            }
            
            if searchQuery != nil {
                Button(action: {
                    searchQuery = nil
                }) {
                    Text(Strings.shared.CANCEL.string())
                        .font(Typography.TitleMedium)
                }
                .padding(.leading, 8)
                .tint(Colors.OnSurface)
                .buttonStyle(.glass)
            }
        }
    }
}

struct SearchInput: View {
    @Binding var text: String
    @FocusState.Binding var isFocused: Bool
    
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 20))
                .foregroundColor(Colors.OnTertiaryContainer)
            
            ZStack(alignment: .leading) {
                if text.isEmpty {
                    Text(Strings.shared.SEARCH.string())
                        .font(Typography.TitleMedium)
                        .foregroundColor(Colors.OnTertiaryContainer.opacity(0.5))
                }
                
                TextField("", text: $text)
                    .font(Typography.TitleMedium)
                    .foregroundColor(Colors.OnTertiaryContainer)
                    .focused($isFocused)
                    .textFieldStyle(.plain)
            }
        }
        .padding(12)
        .background(Colors.TertiaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

#Preview {
    PreviewRoot {
        VStack {
            SearchButtonPreview()
        }
        .padding()
    }
}

struct SearchButtonPreview: View {
    @State private var searchQuery: String? = nil
    
    var body: some View {
        SearchButton(searchQuery: $searchQuery)
    }
}
