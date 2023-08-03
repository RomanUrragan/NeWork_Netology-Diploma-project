package ru.netology.nework.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Post
import ru.netology.nework.error.ApiError
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) :
    ViewModel() {

    val posts: LiveData<List<Post>> =
        combine(postRepository.data, userRepository.data) { posts, users ->
            posts.map { post ->
                post.copy(
                    authorName = users.findLast { user -> user.id == post.authorId }?.name
                        ?: "Author name",
                    mentionUsers = users.filter { user -> post.mentionIds.contains(user.id) }
                )
            }
        }.asLiveData()

    private var _errorLiveData: MutableLiveData<ApiError> = MutableLiveData()
    val errorLiveData: MutableLiveData<ApiError>
        get() = _errorLiveData

    init {
        loadUsers()
        loadPosts()
    }

    fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAll()
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            postRepository.getAll()
        }
    }

    fun onLike(post: Post) {
        viewModelScope.launch {
            try {
                postRepository.onLike(post)
            } catch (e: ApiError) {
                _errorLiveData.value = ApiError(e.status, e.code)
            }
        }
    }
}